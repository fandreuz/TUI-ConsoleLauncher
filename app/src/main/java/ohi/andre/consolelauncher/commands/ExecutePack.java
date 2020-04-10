package ohi.andre.consolelauncher.commands;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.disposables.Disposable;

public abstract class ExecutePack {
    
    public static final String SERVICE_COMMAND_GROUP = "commandGroup";
    
    // the arguments for the last called command
    private Object[] args;
    
    // a set of useful instances shared between the commands.
    // each user can push its own instances after having
    // checked if an instance with the same name is already in the set.
    private Map<String, DisposableWrapper> services;
    
    // the index of the next argument to be readed (if the arguments
    // are read in order)
    public int argumentIndex = 0;
    
    // creates a new ExecutePack using the given CommandGroup
    public ExecutePack (CommandGroup group) {
        services = new HashMap<>();
        
        updateOrPutService(SERVICE_COMMAND_GROUP, new FakeDisposableWrapper(group));
    }
    
    public <T> T getService(String key) {
        return (T) services.get(key);
    }
    
    // fails if a service with the same key is already in the map
    public boolean putService(String key, DisposableWrapper service) {
        if(services.containsKey(key)) return false;
        else {
            services.put(key, service);
            return true;
        }
    }
    
    // fails if there isn't a service with the given key
    public boolean updateService(String key, DisposableWrapper service) {
        if(!services.containsKey(key)) return false;
        else {
            services.put(key, service);
            return true;
        }
    }
    
    public void updateOrPutService(String key, DisposableWrapper service) {
        services.put(key, service);
    }
    
    public <T> T getArgument () {
        if(args != null && argumentIndex < args.length) return (T) args[argumentIndex++];
        else return null;
    }
    
    public <T> T getArgument (int index) {
        if (index < args.length) return (T) args[index];
        return null;
    }
    
    public void setArguments (Object[] args) {
        this.args = args;
    }
    
    // clears the arguments, resets argumentIndex
    public void clear () {
        args         = null;
        argumentIndex = 0;
    }
    
    // associate the interface Disposable to a normal Object
    public abstract static class DisposableWrapper<T> implements Disposable {
        public final T object;
    
        public DisposableWrapper (T object) {
            this.object = object;
        }
    }
    
    // for types which don't need disposal
    public static class FakeDisposableWrapper<T> extends DisposableWrapper {
        public FakeDisposableWrapper (T object) {
            super(object);
        }
    
        @Override
        public boolean isDisposed () {
            return true;
        }
    
        @Override
        public void dispose () {}
    }
}
