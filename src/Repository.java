import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

interface Repository {

    /**
     * Returns the number of entities found.
     *
     * @return the number of entities
     */
    long count();

    /**
     * Returns all the maintained entities.
     *
     * @return all entities
     */
    List<Todo> findAll();

    List<Todo> findAllByStatus(Predicate<Map.Entry<Long, Todo>> entryPredicate);

    /**
     * Returns the entity with the given id.
     *
     * return the entity or {@code null} if not found
     */
    Todo findOne(Long id);

    /**
     * Saves the given entity in the repository, overwriting any previously saved
     * version and <strong>returning a new, possibly modified copy of the saved
     * entity</strong>.
     *
     * @param entity to save
     *
     * @return a new, saved and <strong>possibly modified</strong>, entity or
     *         {@code null} if saving failed
     */
    Todo save(Todo entity);

    /**
     * Deletes the given entity from the repository.
     *
     * @param entity to delete
     */
    int delete(Todo entity);

    /**
     * Deletes the entity identified by the given id.
     *
     * @param id of the entity to delete
     */
    int delete(Long id);

    /**
     * Deletes the given entities.
     *
     * @param entities to delete
     */
    int delete(Collection<Todo> entities);

    int deleteBy(Predicate<Todo> predicate);

    /**
     * Deletes all entities from the repository.
     */
    void deleteAll();

    /**
     * Checks whether an entity with the given id can be found in the repository.
     *
     * @param id of the entity to check for
     *
     * @return {@code true} if an entry with the given id is found, {@code false}
     *         otherwise
     */
    boolean exists(Long id);
}