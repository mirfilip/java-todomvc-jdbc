import java.io.Serializable;

public class Todo implements Serializable {
    public enum Status {
        ACTIVE("active"),
        COMPLETED("completed");

        private final String css;

        Status(String css) {
            this.css = css;
        }

        // TODO: Decouple css styles from internal enums
        @Override
        public String toString() {
            return this.css;
        }
    }

    private final String todo;
    private final Long id;
    private final Status status;

    public Todo(String todo) {
        this.todo = todo;
        this.id = System.currentTimeMillis();
        this.status = Status.ACTIVE;
    }

    public Todo(Long id, String name) {
        this.id = id;
        this.todo = name;
        this.status = Status.ACTIVE;
    }

    public Todo(Long id, String name, Status status) {
        this.id = id;
        this.todo = name;
        this.status = status;
    }

    public Todo(Todo prev, Status newStatus) {
        this.todo = prev.todo;
        this.id = prev.id;
        this.status = newStatus;
    }

    public Long getId() {
        return this.id;
    }

    public String getTodo() {
        return this.todo;
    }

    @Override
    public String toString() {
        return "Todo{" +
                "todo='" + todo + '\'' +
                ", id=" + id +
                ", status=" + status +
                '}';
    }

    public Status getStatus() {
        return this.status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Todo todo1 = (Todo) o;

        if (!todo.equals(todo1.todo)) return false;
        if (!id.equals(todo1.id)) return false;
        return status == todo1.status;

    }

    @Override
    public int hashCode() {
        int result = todo.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }
}