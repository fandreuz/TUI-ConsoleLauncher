package ohi.andre.consolelauncher.tuils;

// defines a runnable which runs with an argument
public abstract class Function<T> implements Runnable {
    public final Class<T> argumentClass;
    protected T argument;

    public Function(Class<T> argumentClass) {
        this.argumentClass = argumentClass;
    }

    // returns this class for a convenient access
    public Function setArgument(T argument) {
        this.argument = argument;
        return this;
    }
}
