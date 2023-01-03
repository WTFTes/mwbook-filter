package net.dramacydal.omegat;

public class MyPair<T, V> {
    protected T key;
    protected V value;

    public MyPair(T key, V value)
    {
        this.key = key;
        this.value = value;
    }

    public T getKey() { return key;}
    public V getValue() { return value;}
}
