package osp.Threads;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

public class ThreadCB extends IflThreadCB {
	static ArrayList<ThreadCB> readyQueue = new ArrayList<ThreadCB>();
	
	public ThreadCB() {
		super();
	}

	/**
		This method will be called once at the beginning of the
		simulation. The student can set up static variables here.
		
		@OSPProject Threads
	*/
	public static void init() {
		
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
		if (task.getThreadCount() >= MaxThreadsPerTask)
			return null;
		ThreadCB thread = new ThreadCB();
		int result = task.addThread(thread);
		if (printableRetCode(result).equals("FAILURE")) {
			return null;
		}
		thread.setTask(task);
		thread.setStatus(ThreadReady);
		readyQueue.add(thread);
		return thread;
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
		if (printableStatus(this.getStatus()).equals("ThreadReady"))
			readyQueue.remove(this);
		else if (printableStatus(this.getStatus()).equals("ThreadRunning"))
			; // remove from controlling the CPU
		for (int i = 0; i < Device.getTableSize(); i++) {
			Device.get(i).cancelPendingIO(this);
		}
		ResourceCB.giveupResources(this);
		do_dispatch();
		this.setStatus(ThreadKill);
		if (this.getTask().getThreadCount() == 0)
			this.getTask().kill();
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
		if (printableStatus(this.getStatus()).equals("ThreadRunning"))
			this.setStatus(ThreadWaiting);
		else if (printableStatus(this.getStatus()).equals("ThreadWaiting"))
			this.setStatus(ThreadWaiting+1);
		do_dispatch();
	}

	/** Resumes the thread.

	Only a thread with the status ThreadWaiting or higher
	can be resumed. The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
	*/
	public void do_resume() {
		if (printableStatus(this.getStatus()).equals("ThreadRunning"))
			readyQueue.add(this);
		else if (printableStatus(this.getStatus()).equals("ThreadWaiting"))
			this.setStatus(ThreadWaiting-1);
		do_dispatch();
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
		if(MMU.getPTBR() != null)
			return FAILURE;
		if(readyQueue.isEmpty())
			return FAILURE;
		//Dequeue from ready queue
		ThreadCB t = readyQueue.remove(0);
		//context switch
		contextSwitch(t);
		return SUCCESS;
	}
	
	private static void contextSwitch(ThreadCB x) {
		//Take CPU away from current thread. We are not using quantums so the status must be ThreadWaiting.
		TaskCB temp = MMU.getPTBR().getTask();
		temp.getCurrentThread().setStatus(ThreadWaiting);
		MMU.setPTBR(null);
		temp.setCurrentThread(null);
		//Give CPU to next thread
		x.setStatus(ThreadRunning);
		MMU.setPTBR(x.getTask().getPageTable());
		x.getTask().setCurrentThread(x);
	}

	/**
		Called by OSP after printing an error message. The student can
		insert code here to print various tables and data structures in
		their state just after the error happened.  The body can be
		left empty, if this feature is not used.

		@OSPProject Threads
	*/
	public static void atError() {	
	
	}

	/** Called by OSP after printing a warning message. The student
		can insert code here to print various tables and data
		structures in their state just after the warning happened.
		The body can be left empty, if this feature is not used.

		@OSPProject Threads
	*/
	public static void atWarning() {
		
	}
}