package com.syzygy.events.database;


import android.util.Log;

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
@Database.Dissovable
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

    @Database.MustStir
    public DatabaseInfLoadQuery(@NonNull @Database.Stirs DatabaseQuery<T> query) {
        this.query = query;
    }


    /**
     * Resets all data and loads the first page of data of this query and loads the new instances
     * @param listener The listener that will be called on completion
     */
    @Override
    @Database.Titrates(what = "Result Instances", when = "All success")
    @Database.StirsDeep(what = "Previous Instances", when = "All success")
    public void refreshData(Listener<DatabaseInfLoadQuery<T>> listener){
        query.gotoFirstPage((query, success) -> {
            if(!success){
                listener.onCompletion(DatabaseInfLoadQuery.this, false);
                return;
            }
            clearInstances();
            addAllInstances();
            Log.d("QueryInf", "Complete Query");
            instances.forEach(i -> Log.d("QueryInf", "\t"+i.getDocumentID()));
            listener.onCompletion(DatabaseInfLoadQuery.this, true);
        });
    }

    /**
     * Adds a page of data to the current list and loads the instances
     * @param listener The listener that will called on completion.
     */
    @Database.Titrates(what = "Result Instances", when = "All success")
    public void incrementData(Listener<DatabaseInfLoadQuery<T>> listener){
        query.gotoNextPage((query, success) -> {
            if(success) addAllInstances();
            listener.onCompletion(DatabaseInfLoadQuery.this, success);
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
    @Database.AutoStir
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
    @Database.Observes
    public List<T> getInstances(){
        return Collections.unmodifiableList(instances);
    }


    /**
     * Clears the set of instances
     */
    @Database.StirsDeep(what = "Results")
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
    @Database.Titrates(what = "All instances")
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
