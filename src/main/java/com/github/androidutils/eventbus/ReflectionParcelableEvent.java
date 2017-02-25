package com.github.androidutils.eventbus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class ReflectionParcelableEvent implements Parcelable {
    public static final Creator<ReflectionParcelableEvent> CREATOR = new Creator<ReflectionParcelableEvent>() {
        @Override
        public ReflectionParcelableEvent createFromParcel(final Parcel source) {
            try {
                final ReflectionParcelableEvent event = (ReflectionParcelableEvent) Class.forName(source.readString())
                        .newInstance();
                List<Field> fields = findAllFields(event);

                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    try {
                        if (Parcelable.class.isAssignableFrom(field.getType())) {
                            field.set(event, source.readParcelable(ClassLoader.getSystemClassLoader()));
                        } else if (String.class.equals(field.getType())) {
                            field.set(event, source.readString());
                        } else if (int.class.equals(field.getType())) {
                            field.set(event, source.readInt());
                        } else if (boolean.class.equals(field.getType())) {
                            field.set(event, source.readInt() == 1);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                }
                return event;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ReflectionParcelableEvent[] newArray(int size) {
            return new ReflectionParcelableEvent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(this.getClass().getName());
        List<Field> fields = findAllFields(this);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                if (Parcelable.class.isAssignableFrom(field.getType())) {
                    dest.writeParcelable((Parcelable) field.get(this), flags);
                } else if (String.class.equals(field.getType())) {
                    dest.writeString((String) field.get(this));
                } else if (int.class.equals(field.getType())) {
                    dest.writeInt(field.getInt(this));
                } else if (boolean.class.equals(field.getType())) {
                    dest.writeInt(field.getBoolean(this) ? 1 : 0);
                }
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append('[');
        List<Field> fields = Arrays.asList(this.getClass().getDeclaredFields());

        for (Iterator<Field> iterator = fields.iterator(); iterator.hasNext();) {
            Field field = iterator.next();
            sb.append(field.getName()).append(' ');
            try {
                sb.append(field.get(this));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (iterator.hasNext()) {
                sb.append(", ");
            } else {
                sb.append(']');
            }
        }
        return sb.toString();
    }

    private static List<Field> findAllFields(final Object object) {
        List<Field> fields = new ArrayList<Field>(Arrays.asList(object.getClass().getDeclaredFields()));
        Class<?> current = object.getClass();
        while (current.getSuperclass() != null) { // we don't want to
                                                  // process
                                                  // Object.class
            // do something with current's fields
            current = current.getSuperclass();
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
        }
        return fields;
    }
}
