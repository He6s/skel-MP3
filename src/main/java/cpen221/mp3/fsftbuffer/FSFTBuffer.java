package cpen221.mp3.fsftbuffer;

import java.util.*;


public class FSFTBuffer<T extends Bufferable> {

    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    private final int capacity;
    private final int timeout;
    private final List<T> stale_time_list = new ArrayList<>();
    private final List<T> recently_used_list = new ArrayList<>();
    private final List<Long> stale_time = new ArrayList<>();
    private final List<Long> recently_used_time = new ArrayList<>();

    /**
     * Create a buffer with a fixe
     * d capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, int timeout) {
        this.capacity = capacity;
        this.timeout = timeout;
    }

    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this(DSIZE, DTIMEOUT);
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     */
    public synchronized boolean put(T t) {
        check();

        if (stale_time_list.size() > capacity) {
            return false;
        }

        // remove the least recently accessed object from the buffer
        else if (stale_time_list.size() == capacity) {
            for (int stale_index = 0; stale_index < stale_time_list.size(); stale_index++) {
                if (stale_time_list.get(stale_index).equals(recently_used_list.get(0))) {
                    stale_time_list.remove(stale_index);
                    stale_time.remove(stale_index);
                    break;
                }
            }

            recently_used_time.remove(0);
            recently_used_list.remove(0);
        }

        stale_time_list.add(t);
        stale_time.add(System.currentTimeMillis());
        recently_used_time.add(System.currentTimeMillis());
        recently_used_list.add(t);

        return true;
    }

    /**
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the
     * buffer
     */
    public synchronized T get(String id) throws IllegalAccessException {
        check();

        // renew recently used time
        for (int t_index = 0; t_index < recently_used_list.size(); t_index++) {
            if (recently_used_list.get(t_index).id().equals(id)) {
                recently_used_list.add(recently_used_list.get(t_index));
                recently_used_list.remove(t_index);

                recently_used_time.remove(stale_time.get(t_index));
                recently_used_time.add(System.currentTimeMillis());

                break;
            }
        }

        for (T t_index : stale_time_list) {
            if (t_index.id().equals(id)) {
                return t_index;
            }
        }

        throw new IllegalAccessException();
        /* Do not return null. Throw a suitable checked exception when an object
            is not in the cache. You can add the checked exception to the method
            signature. */
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public synchronized boolean touch(String id) {
        check();

        boolean truth_value = false;

        // update stale time
        for (int id_index = 0; id_index < stale_time_list.size(); id_index++) {
            if (Objects.equals(stale_time_list.get(id_index).id(), id)) {
                stale_time_list.add(stale_time_list.get(id_index));
                stale_time_list.remove(id_index);

                stale_time.remove(id_index);
                stale_time.add(System.currentTimeMillis());

                truth_value = true;

                break;
            }
        }

        // update recently used time
        for (int id_index = 0; id_index < recently_used_time.size(); id_index++) {
            if (Objects.equals(recently_used_list.get(id_index).id(), id)) {
                recently_used_list.add(recently_used_list.get(id_index));
                recently_used_list.remove(id_index);

                recently_used_time.remove(id_index);
                recently_used_time.add(System.currentTimeMillis());

                break;
            }
        }
        return truth_value;
    }

    /**
     * Update an object in the buffer.
     * This method updates an object and acts like a "touch" to
     * renew the object in the cache.
     *
     * @param t the object to update
     * @return true if successful and false otherwise
     */
    public synchronized boolean update(T t) {
        check();

        boolean truth_value = false;

        for (int t_index = 0; t_index < stale_time_list.size(); t_index++) {
            if (t.id().equals(stale_time_list.get(t_index).id())) {
                stale_time_list.add(t);
                stale_time_list.remove(t_index);

                stale_time.remove(t_index);
                stale_time.add(System.currentTimeMillis());

                truth_value = true;

                break;
            }
        }

        for (int t_index = 0; t_index < recently_used_time.size(); t_index++) {
            if (t.id().equals(recently_used_list.get(t_index).id())) {
                recently_used_list.add(t);
                recently_used_list.remove(t_index);

                recently_used_time.remove(t_index);
                recently_used_time.add(System.currentTimeMillis());

                break;
            }
        }

        return truth_value;
    }

    private synchronized void check() {
        for (int time_index = 0; time_index < stale_time.size(); time_index++) {
            if (System.currentTimeMillis() - stale_time.get(time_index) > timeout * 1000L) {
                for (int every_RU = 0; every_RU < recently_used_time.size(); every_RU++) {
                    if (recently_used_list.get(every_RU).equals(stale_time_list.get(time_index))) {
                        recently_used_list.remove(every_RU);
                        recently_used_time.remove(every_RU);
                        break;
                    }
                }

                stale_time.remove(stale_time.get(time_index));
                stale_time_list.remove(stale_time_list.get(time_index));
            }
        }
    }
}
