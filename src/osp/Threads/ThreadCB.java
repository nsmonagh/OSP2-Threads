package osp.Threads;

import java.util.ArrayList;

import osp.Devices.Device;
import osp.IFLModules.Event;
import osp.IFLModules.IflThreadCB;
import osp.Memory.MMU;
import osp.Resources.ResourceCB;
import osp.Tasks.TaskCB;

public class ThreadCB extends IflThreadCB {
	static ArrayList<ThreadCB> readyQueue;
	
	public ThreadCB() {
		super();
	}

	public static void init() {
		 readyQueue = new ArrayList<ThreadCB>();
	}

	/** 
		Sets up a new thread and adds it to the given task. 
		The method must set the ready status 
		and attempt to add thread to task. If the latter fails 
		because there are already too many threads in this task, 
		so does this method, otherwise, the thread is appended 
		to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

		@OSPProject Threads
	*/
	static public ThreadCB do_create(TaskCB task) {
		
        ThreadCB thread = null;
        
        if(task == null||(task.getThreadCount() >= MaxThreadsPerTask)){
        	ThreadCB.dispatch();
        	return null;
        }
        
        thread = new ThreadCB();                            
        thread.setPriority(task.getPriority());             
        thread.setStatus(ThreadReady);                     
        thread.setTask(task); 
        
        if(task.addThread(thread) == 0){
            ThreadCB.dispatch();
            return null;
        }
        
        readyQueue.add(thread);                        
        ThreadCB.dispatch();                               
        return thread;

        //old attempt
		/*if (task.getThreadCount() >= MaxThreadsPerTask)
			return null;
		ThreadCB thread = new ThreadCB();
		int result = task.addThread(thread);
		if (printableRetCode(result).equals("FAILURE")) {
			return null;
		}
		thread.setTask(task);
		thread.setStatus(ThreadReady);
		readyQueue.add(thread);
		ThreadCB.dispatch();
		return thread;
		*/
	}

	/** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.

	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
	*/
	public void do_kill() {
		int status = getStatus();
		TaskCB task = getTask();
		if (status == ThreadReady)
			readyQueue.remove(this);
		else if (status == ThreadRunning) {
			MMU.setPTBR(null);
			task.setCurrentThread(null);
		}
		for (int i = 0; i < Device.getTableSize(); i++) {
			Device.get(i).cancelPendingIO(this);
		}
		ResourceCB.giveupResources(this);
		dispatch();
		this.setStatus(ThreadKill);
		if (task.getThreadCount() == 0)
			task.kill();
		this.setStatus(ThreadRunning);
	}

	/** Suspends the thread that is currently on the processor on the 
		specified event. 

		Note that the thread being suspended doesn't need to be
		running. It can also be waiting for completion of a pagefault
		and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
		the processor set to idle, the thread must be in the right
		waiting queue, and dispatch() must be called to give CPU
		control to some other thread.

	@param event - event on which to suspend this thread.

		@OSPProject Threads
	*/
	public void do_suspend(Event event) {
		int status = getStatus();
		if (status == ThreadRunning) {
			setStatus(ThreadWaiting);
			MMU.setPTBR(null);
			getTask().setCurrentThread(null);
		}
		else if (status >= ThreadWaiting)
			setStatus(++status);
		event.addThread(this);
		dispatch();
	}

	/** Resumes the thread.

	Only a thread with the status ThreadWaiting or higher
	can be resumed. The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
	*/
	public void do_resume() {
		int status = getStatus();
		if (status == ThreadWaiting) {
			setStatus(ThreadReady);
			readyQueue.add(this);
		}
		else
			setStatus(--status);
		dispatch();
	}

	/** 
		Selects a thread from the run queue and dispatches it. 

		If there is just one thread ready to run, reschedule the thread 
		currently on the processor.

		In addition to setting the correct thread status it must
		update the PTBR.
	
	@return SUCCESS or FAILURE

		@OSPProject Threads
	*/
	public static int do_dispatch() {
		if(readyQueue.isEmpty())
			return FAILURE;
		if(MMU.getPTBR() == null) { //No running thread, no need for context switch
			ThreadCB x = readyQueue.remove(0);
			//Give CPU to thread
			x.setStatus(ThreadRunning);
			MMU.setPTBR(x.getTask().getPageTable());
			x.getTask().setCurrentThread(x);
			return SUCCESS;
		}
		//Otherwise. dequeue from ready queue and perform context switch
		ThreadCB t = readyQueue.remove(0);
		//Take CPU away from current thread. We are not using quantums so the status must be ThreadWaiting.
		TaskCB temp = MMU.getPTBR().getTask();
		temp.getCurrentThread().setStatus(ThreadWaiting);
		MMU.setPTBR(null);
		temp.setCurrentThread(null);
		//Give CPU to next thread
		t.setStatus(ThreadRunning);
		MMU.setPTBR(t.getTask().getPageTable());
		t.getTask().setCurrentThread(t);
		return SUCCESS;
	}

	public static void atError() {}
	public static void atWarning() {}
}