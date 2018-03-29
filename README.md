Sim: 146592 <<Warning!>> [IFLModules.ThreadCB]  
	After interrupt(PageFault): dispatch() should have been called

	at osp.IFLModules.IflThreadCB.M(IflThreadCB.java:577)
	at osp.Hardware.CPU.interrupt(CPU.java:56)
	at osp.Memory.MMU.if(MMU.java:159)
	at osp.IFLModules.IflMMU.a(IflMMU.java:220)
	at osp.IFLModules.ReferThread.run(ReferThread.java:61)

Sim: 235200 <<Error!>> [IFLModules.IflMMU]  
	Frame(6) says that it holds Page(5:8/null) but the page says that it's frame is null

	at osp.IFLModules.IflMMU.ae(IflMMU.java:516)
	at osp.IFLModules.IflPageFaultHandler.a(IflPageFaultHandler.java:144)
	at osp.IFLModules.IflPageFaultHandler.handleInterrupt(IflPageFaultHandler.java:44)
	at osp.Interrupts.Interrupts.interrupt(Interrupts.java:48)
	at osp.Hardware.CPU.interrupt(CPU.java:54)
	at osp.Memory.MMU.if(MMU.java:159)
	at osp.IFLModules.IflMMU.a(IflMMU.java:220)
	at osp.IFLModules.ReferThread.run(ReferThread.java:61)