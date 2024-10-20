package com.syzygy.events.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

/**
 * A database instance which contains database instances of a certain type
 * <p>
 *     Supports duplicates. Does not support {@code null}
 * </p>
 * @author Gareth Kmet
 * @version 1.0
 * @since 19oct24
 * @param <T> The type of the database instance to be stored
 * TODO remove as database instance
 */
public class DatabaseInstanceList<T extends DatabaseInstance<T>> implements List<T>{

    private List<T> list;

    DatabaseInstanceList(){

    }

    public void fullDissolve() {
        list.forEach(DatabaseInstance::dissolve);
        list.clear();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return list.contains(o);
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull T1[] t1s) {
        return list.toArray(t1s);
    }

    @Override
    public boolean add(@NonNull T t) {
        return list.add(t.fetch());
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(@Nullable Object o) {
        if(o == null) return false;
        boolean t = list.remove(o);
        if(t){
            ((DatabaseInstance<T>)o).dissolve();
        }
        return t;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return list.containsAll(collection);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> collection) {
        collection.forEach(DatabaseInstance::fetch);
        return list.addAll(collection);
    }

    @Override
    public boolean addAll(int i, @NonNull Collection<? extends T> collection) {
        collection.forEach(DatabaseInstance::fetch);
        return list.addAll(i, collection);
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        throw new UnsupportedOperationException();
        // TODO implement
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        throw new UnsupportedOperationException();
        // TODO implement

    }

    @Override
    public void clear() {
        list.forEach(DatabaseInstance::dissolve);
        list.clear();
    }

    @Override
    public T get(int i) {
        return list.get(i);
    }

    @Override
    public T set(int i, @NonNull T t) {
        T told = get(i);
        if(told != null){
            told.dissolve();
        }
        return list.set(i,t);
    }

    @Override
    public void add(int i, @NonNull T t) {
        list.add(i,t.fetch());
    }

    @Override
    public T remove(int i) {
        T t = list.remove(i);
        t.dissolve();
        return t;
    }

    @Override
    public int indexOf(@Nullable Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
        return list.lastIndexOf(o);
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator(int i) {
        return list.listIterator(i);
    }

    @NonNull
    @Override
    public List<T> subList(int i, int i1) {
        return list.subList(i,i1);
    }

    @Override
    public void forEach(@NonNull Consumer<? super T> action) {
        list.forEach(action);
    }
}
