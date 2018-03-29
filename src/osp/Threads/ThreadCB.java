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

	public void do_resume() {
		int status = getStatus();
		if (status == ThreadWaiting) {
			setStatus(ThreadReady);
			readyQueue.append(this);
		}
		else if(status > ThreadWaiting)
			setStatus(--status);
		else return;
		dispatch();
	}

	public static int do_dispatch() {
		try {
			ThreadCB thread = MMU.getPTBR().getTask().getCurrentThread();
			if (thread != null) {
				thread.getTask().setCurrentThread(null);
				MMU.setPTBR(null);
				thread.setStatus(ThreadReady);
				readyQueue.append(thread);
			}
		} catch (NullPointerException e) {}
		if (readyQueue.isEmpty()) {
			MMU.setPTBR(null);
			return FAILURE;
		}
		ThreadCB head = (ThreadCB) readyQueue.removeHead();
		MMU.setPTBR(head.getTask().getPageTable());
		head.getTask().setCurrentThread(head);
		head.setStatus(ThreadRunning);
		return SUCCESS;
	}

	public static void atError() {}
	public static void atWarning() {}
}