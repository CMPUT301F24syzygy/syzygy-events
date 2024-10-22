package com.syzygy.events.database;


import androidx.annotation.NonNull;

import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores a query that is executed on a collection within the database. Loads instances returned from this query by pages compounding
 * @author Gareth Kmet
 * @version 1.0
 * @since 21oct24
 * @param <T> The type of instance being returned
 * @see DatabaseQuery
 */
public class DatabaseInfLoadQuery<T extends DatabaseInstance<T>> implements Database.UpdateListener, Database.Querrier<DatabaseInfLoadQuery<T>> {

    /**
     * The database query object
     */
    @NonNull
    private final DatabaseQuery<T> query;

    /**
     * If an item from the current set of instances has been deleted or updated
     */
    private boolean outOfDate = false;

    /**
     * The set of instances
     */
    private final List<T> instances = new ArrayList<>();

    public DatabaseInfLoadQuery(@NonNull DatabaseQuery<T> query) {
        this.query = query;
    }


    /**
     * Resets all data and loads the first page of data of this query and loads the new instances
     * @param listener The listener that will be called on completion
     */
    @Override
    public void refreshData(Listener<DatabaseInfLoadQuery<T>> listener){
        query.gotoFirstPage(new Listener<DatabaseQuery<T>>() {
            @Override
            public void onError(DatabaseQuery<T> query) {
                listener.onError(DatabaseInfLoadQuery.this);
            }

            @Override
            public void onSuccess(DatabaseQuery<T> query) {
                clearInstances();
                addAllInstances();
                listener.onSuccess(DatabaseInfLoadQuery.this);
            }
        });
    }

    /**
     * Adds a page of data to the current list and loads the instances
     * @param listener The listener that will called on completion.
     */
    public void incrementData(Listener<DatabaseInfLoadQuery<T>> listener){
        query.gotoNextPage(new Listener<DatabaseQuery<T>>() {
            @Override
            public void onError(DatabaseQuery<T> query) {
                listener.onError(DatabaseInfLoadQuery.this);
            }

            @Override
            public void onSuccess(DatabaseQuery<T> query) {
                addAllInstances();
                listener.onSuccess(DatabaseInfLoadQuery.this);
            }
        });
    }

    /**
     * If an item from the current set of instances has been deleted or updated
     * @return {@code true} if an item has been deleted or updated
     */
    public boolean isOutOfDate(){
        return outOfDate;
    }

    /**
     * Removes references to all instances that have been created and clears the current instance
     */
    public void dissolve(){
        query.dissolve();
        clearInstances();
    }

    /**
     * Returns true if there is more data to load (to the knowledge of this list)
     * <p>
     *     If the last result returned a full page, this assumes there is more to load
     * </p>
     * @return {@code true} if there is more data to load
     */
    public boolean hasUnloadedData(){
        return !query.isLastPage();
    }

    /**
     * @return An unmodifiable list of the current instances loaded
     */
    @Unmodifiable
    @NonNull
    public List<T> getInstances(){
        return Collections.unmodifiableList(instances);
    }


    /**
     * Clears the set of instances
     */
    private void clearInstances(){
        instances.forEach(i -> {
            i.dissolve(this);
        });
        instances.clear();
        outOfDate = false;
    }

    /**
     * Adds all the instances in the current page to this set
     */
    private void addAllInstances(){
        query.getCurrentInstances().forEach(i -> instances.add(i.fetch(this)));
    }

    @Override
    public <S extends DatabaseInstance<S>> void onUpdate(DatabaseInstance<S> instance, Type type) {
        if(type == Type.DELETE || type == Type.UPDATE){
            outOfDate = true;
        }
    }
}
