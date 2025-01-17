package org.perl6.nqp.runtime;

import org.perl6.nqp.sixmodel.KnowHOWBootstrapper;
import org.perl6.nqp.sixmodel.SixModelObject;

public class GlobalContext {
    /**
     * The KnowHOW.
     */
    public SixModelObject KnowHOW;
    
    /**
     * The KnowHOWAttribute.
     */
    public SixModelObject KnowHOWAttribute;
    
    /**
     * BOOTArray type; a basic, method-less type with the VMArray REPR.
     */
    public SixModelObject BOOTArray;
    
    /**
     * BOOTHash type; a basic, method-less type with the VMHash REPR.
     */
    public SixModelObject BOOTHash;
    
    /**
     * BOOTInt type; a basic, method-less type with the P6int REPR.
     */
    public SixModelObject BOOTInt;
    
    /**
     * BOOTNum type; a basic, method-less type with the P6num REPR.
     */
    public SixModelObject BOOTNum;
    
    /**
     * BOOTStr type; a basic, method-less type with the P6str REPR.
     */
    public SixModelObject BOOTStr;
    
    /**
     * The main, startup thread's ThreadContext.
     */
    public ThreadContext mainThread;
    
    /**
     * Initializes the runtime environment.
     */
    public GlobalContext()
    {
        mainThread = new ThreadContext(this);
        KnowHOWBootstrapper.bootstrap(mainThread);
    }
}
