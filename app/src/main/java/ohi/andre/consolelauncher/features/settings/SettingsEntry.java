package ohi.andre.consolelauncher.features.settings;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * Created by francescoandreuzzi on 06/03/2018.
 */

/*

 */
public class SettingsEntry {

    // the option which observes this entry
    public final SettingsOption option;

    // the value assigned by the user
    private String value;

    private BehaviorSubject subject;

    // instructs BehaviorSubject about the type of the argument to be broadcasted
    private Class<?> valueClass;

    public SettingsEntry(SettingsOption option, String value) {
        this.option = option;
        this.value = value;
    }

    public String get() {
        return value;
    }

    protected void set(String value) {
        this.value = value;

        if(subject != null) {
            subject.onNext(value);
        }
    }

    // draft a subscription to this entry
    protected <T> Observable subscribe(Class<T> clazz) {
        if(subject == null) {
            this.valueClass = clazz;
            subject = BehaviorSubject.create();
        }

        return subject.map((Function<String, T>) o -> (T) SettingsManager.transform(o, valueClass));
    }

    @Override
    public String toString() {
        return option.label() + " --> " + value;
    }
}