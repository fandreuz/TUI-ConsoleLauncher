package ohi.andre.consolelauncher.managers.settings;

import io.reactivex.rxjava3.core.Observer;
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
            subject.onNext(SettingsManager.transform(value, valueClass));
        }
    }

    protected <T> void subscribe(Class<?> clazz, Observer observer) {
        if(subject == null) {
            this.valueClass = clazz;
            subject = BehaviorSubject.create();
        }

        subject.subscribe(observer);
    }

    @Override
    public String toString() {
        return option.label() + " --> " + value;
    }
}