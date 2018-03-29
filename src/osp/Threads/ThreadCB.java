package osp.Threads;

import osp.Devices.Device;
import osp.IFLModules.Event;
import osp.IFLModules.IflThreadCB;
import osp.Memory.MMU;
import osp.Resources.ResourceCB;
import osp.Tasks.TaskCB;
import osp.Utilities.GenericList;

public class ThreadCB extends IflThreadCB {
	static GenericList readyQueue;
	
	public ThreadCB() {
		super();
	}

	public static void init() {
		 readyQueue = new GenericList();
	}

	static public ThreadCB do_create(TaskCB task) {
		if (task == null || (task.getThreadCount() >= MaxThreadsPerTask)) {
			ThreadCB.dispatch();
			return null;
		}
		ThreadCB thread = new ThreadCB();
		thread.setPriority(task.getPriority());
		thread.setStatus(ThreadReady);
		thread.setTask(task);
		if (task.addThread(thread) == FAILURE) {
			ThreadCB.dispatch();
			return null;
		}
		readyQueue.append(thread);
		ThreadCB.dispatch();
		return thread;
	}

	public void do_kill() {
		int status = getStatus();
		TaskCB task = getTask();
		if (status == ThreadReady)
			readyQueue.remove(this);
		else if (status == ThreadRunning) {
			try {
				if (MMU.getPTBR().getTask().getCurrentThread() == this) {
					MMU.setPTBR(null);
					task.setCurrentThread(null);
				}
			} catch (NullPointerException e) {}
		}
		getTask().removeThread(this);
		setStatus(ThreadKill);
		for (int i = 0; i < Device.getTableSize(); i++)
			Device.get(i).cancelPendingIO(this);
		ResourceCB.giveupResources(this);
		dispatch();
		if (task.getThreadCount() == 0)
			task.kill();
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
			try {
				if (MMU.getPTBR().getTask().getCurrentThread() == this) {
					MMU.setPTBR(null);
					getTask().setCurrentThread(null);
					setStatus(ThreadWaiting);
				}
			} catch (NullPointerException e) {}
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
		if (getStatus() == ThreadWaiting) {
			setStatus(ThreadReady);
			readyQueue.append(this);
		}
		else if(getStatus() > ThreadWaiting)
			setStatus(getStatus()-1);
		else return;
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
		ThreadCB x = (ThreadCB) readyQueue.removeHead();
		if(MMU.getPTBR() == null) { //No running thread, no need for context switch
			//Give CPU to thread
			x.setStatus(ThreadRunning);
			MMU.setPTBR(x.getTask().getPageTable());
			x.getTask().setCurrentThread(x);
			return SUCCESS;
		}
		else {
			//Otherwise, dequeue from ready queue and perform context switch
			//Take CPU away from current thread. We are not using quantums so the status must be ThreadWaiting.
			TaskCB temp = MMU.getPTBR().getTask();
			temp.getCurrentThread().setStatus(ThreadWaiting);
			MMU.setPTBR(null);
			temp.setCurrentThread(null);
			//Give CPU to next thread
			x.setStatus(ThreadRunning);
			MMU.setPTBR(x.getTask().getPageTable());
			x.getTask().setCurrentThread(x);
			return SUCCESS;
		}
	}

	public static void atError() {}
	public static void atWarning() {}
}