import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RdbmsRepository implements Repository {
    // http://stackoverflow.com/questions/2225221/closing-database-connections-in-java
    private DataSource dataSource;

    public RdbmsRepository(DataSource dataSource) throws Exception {
        this.dataSource = dataSource;
        deleteAll();
    }

    @Override
    public long count() {
        /*
         * Autoclosable resources
         * @see https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
         * @see http://stackoverflow.com/a/5783082
         * @see https://accu.org/index.php/journals/236
         */
        try (Statement statement = dataSource.getConnection().createStatement()) {
            ResultSet result = statement.executeQuery("SELECT COUNT(*) AS total FROM todos");
            result.next();

            return result.getInt("total");
        } catch (SQLException e) {
            System.out.println("count failed: " + e.getMessage());
            return -1L;
        }
    }

    @Override
    public List<Todo> findAll() {
        List<Todo> todosList = new ArrayList<>();

        /*
         * Autoclosable resources
         * @see https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
         * @see http://stackoverflow.com/a/5783082
         * @see https://accu.org/index.php/journals/236
         */
        try (Statement statement = dataSource.getConnection().createStatement()) {
            ResultSet result = statement.executeQuery(
                    "SELECT id, name, status "
                    + "FROM todos "
                    + "ORDER BY id DESC"
            );

            while(result.next()) {
                Long id = result.getLong("id");
                String name = result.getString("name");
                Todo.Status status = Todo.Status.valueOf(result.getString("status").toUpperCase());

                todosList.add(new Todo(id, name, status));
            }

            return todosList;
        } catch (SQLException e) {
            System.out.println("findAll failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Todo> findAllByStatus(Predicate<Map.Entry<Long, Todo>> entryPredicate) {
        // Convert a list to a map
//        final Map<Long, Todo> internalMap = new HashMap<>();
//        for (final Todo todo : findAll()) {
//            internalMap.put(todo.getId(), todo);
//        }

        // Convert a list to map using streams
//        Map<Long, Todo> internalMap = findAll().stream().collect(Collectors.toMap(Todo::getId, Function.identity()));
//
//        return internalMap.entrySet()
//                .stream()
//                .filter(entryPredicate)
//                .map(Map.Entry::getValue)
//                .collect(Collectors.toList());

        List<Todo> all = findAll();

        final Map<Long, Todo> internalMap = all
                .stream()
                .collect(Collectors.toMap(i -> i.getId(), i -> i));

        return internalMap
                .entrySet().stream()
                .filter(entryPredicate)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public Todo findOne(Long id) {
        /*
         * Autoclosable resources
         * @see https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
         * @see http://stackoverflow.com/a/5783082
         * @see https://accu.org/index.php/journals/236
         */
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = createFindOnePreparedStatement(connection, id);
             ResultSet result = statement.executeQuery()
        ) {
            if (result.next()) {
                String name = result.getString("name");
                Todo.Status status = Todo.Status.valueOf(result.getString("status").toUpperCase());

                return new Todo(id, name, status);
            } else {
                return null;
            }

        } catch (SQLException e) {
            System.out.println("findOne failed: " + e.getMessage());
            return null;
        }
    }

    private PreparedStatement createFindOnePreparedStatement(Connection connection, Long id) {
        String sql = "SELECT name, status "
                + "FROM todos "
                + "WHERE id = ?";

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            ps.setLong(1, id);
        } catch (SQLException e) {
            System.out.println("creating findOne prepared statement failed: " + e.getMessage());
        }

        return ps;
    }

    @Override
    public Todo save(Todo entity) {
        if (exists(entity.getId())) {
            return executeUpdate(entity);
        } else {
            return executeInsert(entity);
        }
    }

    private Todo executeInsert(Todo entity) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = createInsertPreparedStatement(connection)
        ) {
            statement.setLong(1, entity.getId());
            statement.setString(2, entity.getTodo());
            statement.setString(3, entity.getStatus().toString());

            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Inserting todo " + entity.toString() + " failed: " + e.getMessage());
            return null;
        }

        return entity;
    }

    private Todo executeUpdate(Todo entity) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = createUpdatePreparedStatement(connection)
        ) {
            statement.setString(1, entity.getTodo());
            statement.setString(2, entity.getStatus().toString());
            statement.setLong(3, entity.getId());

            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Updating todo " + entity.toString() + " failed: " + e.getMessage());
            return null;
        }

        return entity;
    }

    private PreparedStatement createInsertPreparedStatement(Connection connection) {
        String sql = "INSERT INTO todos (id, name, status) VALUE (?, ?, ?)";

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
        } catch (SQLException e) {
            System.out.println("creating insert prepared statement failed: " + e.getMessage());
        }

        return ps;
    }

    private PreparedStatement createUpdatePreparedStatement(Connection connection) {
        String sql = "UPDATE todos SET name = ?, status = ? "
                + "WHERE id = ?" ;

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
        } catch (SQLException e) {
            System.out.println("creating update prepared statement failed: " + e.getMessage());
        }

        return ps;
    }

    @Override
    public int delete(Todo entity) {
        return delete(entity.getId());
    }

    @Override
    public int delete(Long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = createDeletePreparedStatement(connection)
        ) {
            statement.setLong(1, id);
            return statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Deleting todo " + id.toString() + " failed: " + e.getMessage());
            return -1;
        }
    }

    private PreparedStatement createDeletePreparedStatement(Connection connection) {
        String sql = "DELETE FROM todos WHERE id = ?";

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
        } catch (SQLException e) {
            System.out.println("creating delete prepared statement failed: " + e.getMessage());
        }

        return ps;
    }

    @Override
    public int delete(Collection<Todo> entities) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = createDeletePreparedStatement(connection)
        ) {
            int deleted = 0;
            for (Todo todo : entities) {
                statement.setLong(1, todo.getId());
                deleted += statement.executeUpdate();
            }
            return deleted;
        } catch (SQLException e) {
            System.out.println("Deleting collection of todos " + entities + " failed: " + e.getMessage());
            return -1;
        }
    }

    @Override
    public int deleteBy(Predicate<Todo> predicate) {
        Collection<Todo> allTodos = findAll();

        // Todo: Delete collection in one go
        return (int) allTodos.stream()
                .filter(predicate)
                .map(todo -> delete(todo.getId()))
                .filter(i -> i == 1).count();
    }

    @Override
    public void deleteAll() {
        try (Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute("DELETE FROM todos");
        } catch (SQLException e) {
            System.out.println("clearing repository failed: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(Long id) {
        return null != findOne(id);
    }
}
