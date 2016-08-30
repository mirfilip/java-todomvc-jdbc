import org.apache.commons.dbcp2.BasicDataSource;

public class Main {
    public static void main(String[] args) throws Exception {
        String dbUrl = "jdbc:mysql://localhost:3306/todomvc" +
                "?verifyServerCertificate=false" +
                "&useSSL=false" +
                "&useUnicode=true" +
                "&serverTimezone=UTC";

        BasicDataSource dataSource = new BasicDataSource();

        String dbUsername = "techjump";
        String dbPassword = "techjump";

        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);

        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setInitialSize(5);

        Server server = new JdkHttpServer(new Application(new RdbmsRepository(dataSource)));

        server.run();
    }
}
