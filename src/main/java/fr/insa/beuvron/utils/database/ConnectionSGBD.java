/*
    Copyright 2000-2014 Francois de Bertrand de Beuvron

    This file is part of UtilsBeuvron.

    UtilsBeuvron is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UtilsBeuvron is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UtilsBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.beuvron.utils.database;

import fr.insa.beuvron.utils.ConsoleFdB;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Une classe qui contient une {@link java.sql.Connection} avec en plus des
 * informations spécifiques au type de SGBD (postgres, mysql ...).
 *
 * @author francois
 */
public class ConnectionSGBD implements AutoCloseable {
    
    @Override
    public void close() throws SQLException {
        if (this.con != null) {
            this.con.close();
        }
    }

    /**
     * informations de base pour pouvoir se connecter par jdbc
     */
    public interface InfosPourConnection {

        /**
         *
         * @return
         */
        public String getDriverFullClassName();

        /**
         *
         * @return
         */
        public String getDriverType();

        /**
         *
         * @return
         */
        public int getDefaultPort();

        /**
         *
         * @param hostName
         * @param hostPort
         * @param databaseName
         * @return
         */
        public default String getUrl(String hostName, int hostPort, String databaseName) {
            return "jdbc:" + this.getDriverType() + "://"
                    + hostName + ":" + hostPort + "/"
                    + databaseName;
        }

        /**
         * create a connection to the sgbd. Connection are created by default as
         * TRANSACTION_SERIALIZABLE
         */
        public default Connection connect(String host, int port,
                String database,
                String user, String pass) throws SQLException {
            try {
                Class.forName(this.getDriverFullClassName());
            } catch (ClassNotFoundException ex) {
                throw new SQLException(ex);
            }
            Connection con = DriverManager.getConnection(
                    this.getUrl(host, port, database),
                    user, pass);
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            return con;
        }
    }

    /**
     * Certaines constructions dépendent du SGBD (soit elle ne sont pas dans le
     * standard SQL, soit certains SGBD ne respectent pas le standard). Nous
     * essayons de rassembler dans cette interface les constructions
     * non-standard que nous utilisons.
     */
    public interface SQLSpecificSGBD {

        /**
         * défini les (souvent le) caractères entourant les noms (noms de
         * tables, noms de colonnes ...) en SQL. En général le guillemet ".
         * Devra être spécialisée pour mySQL/mariaDB qui utilisent le backquote
         * `
         */
        public default String entoureNom() {
            return "\"";
        }

        public default String quoteNom(String nom) {
            String sep = entoureNom();
            // il faut doubler le séparateur s'il apparait dans le nom
            nom = nom.replace(sep, sep + sep);
            // puis entourer le nom par le séparateur
            return sep + nom + sep;
        }
        
        public String sqlForGeneratedIntPKColumn(String columnName);
        
        public String sqlForConcat(String sqlForS1, String sqlForS2);
    }
    
    public interface SGBDDef extends InfosPourConnection, SQLSpecificSGBD {
    }

    /**
     * donne les valeurs par défaut pour divers SGBD
     */
    public enum SGBDConnus implements SGBDDef {
        
        POSTGRESQL() {
            @Override
            public String getDriverFullClassName() {
                return "org.postgresql.Driver";
            }
            
            @Override
            public String getDriverType() {
                return "postgresql";
            }
            
            @Override
            public int getDefaultPort() {
                return 5432;
            }
            
            @Override
            public String sqlForGeneratedIntPKColumn(String columnName) {
                return columnName + " INTEGER " + " PRIMARY KEY "
                        + " GENERATED ALWAYS AS IDENTITY ";
            }
            
            @Override
            public String sqlForConcat(String sqlForS1, String sqlForS2) {
                return sqlForS1 + " || " + sqlForS2;
            }
        },
        MARIADB() {
            @Override
            public String getDriverFullClassName() {
                return "org.mariadb.jdbc.Driver";
            }
            
            @Override
            public String getDriverType() {
                return "mariadb";
            }
            
            @Override
            public int getDefaultPort() {
                return 3306;
            }
            
            @Override
            public String entoureNom() {
                return "`";
            }
            
            @Override
            public String sqlForGeneratedIntPKColumn(String columnName) {
                return columnName + " INTEGER " + " PRIMARY KEY "
                        + " AUTO_INCREMENT ";
            }
            
            @Override
            public String sqlForConcat(String sqlForS1, String sqlForS2) {
                return "CONCAT(" + sqlForS1 + " , " + sqlForS2 + ")";
            }
        },
        MYSQL() {
            @Override
            public String getDriverFullClassName() {
                return "com.mysql.cj.jdbc.Driver";
            }
            
            @Override
            public String getDriverType() {
                return "mysql";
            }
            
            @Override
            public int getDefaultPort() {
                return 3306;
            }
            
            @Override
            public String entoureNom() {
                return "`";
            }
            
            @Override
            public String sqlForGeneratedIntPKColumn(String columnName) {
                return columnName + " INTEGER " + " PRIMARY KEY "
                        + " AUTO_INCREMENT ";
            }
            
            @Override
            public String sqlForConcat(String sqlForS1, String sqlForS2) {
                return "CONCAT(" + sqlForS1 + " , " + sqlForS2 + ")";
            }
        },
        SQLITE() {
            @Override
            public String getDriverFullClassName() {
                return "org.sqlite.JDBC";
            }
            
            @Override
            public String getDriverType() {
                return "sqlite";
            }
            
            @Override
            public int getDefaultPort() {
                throw new Error("no port for SQLITE : direct access to file system");
            }
            
            @Override
            public String getUrl(String hostName, int hostPort, String databaseName) {
                return "jdbc:sqlite:" + databaseName;
            }
            
            @Override
            public String sqlForGeneratedIntPKColumn(String columnName) {
                return columnName + " INTEGER " + " PRIMARY KEY "
                        + " AUTOINCREMENT ";
            }
            
            @Override
            public String sqlForConcat(String sqlForS1, String sqlForS2) {
                return "CONCAT(" + sqlForS1 + " , " + sqlForS2 + ")";
            }
        },
        H2LocalFile() {
            @Override
            public String getDriverFullClassName() {
                return "org.h2.Driver";
            }
            
            @Override
            public String getDriverType() {
                return "h2";
            }
            
            @Override
            public int getDefaultPort() {
                throw new Error("no port for H2 in local file mode : direct access to file system");
            }
            
            @Override
            public String getUrl(String hostName, int hostPort, String databaseName) {
                return "jdbc:h2:file:" + databaseName;
            }
            
            @Override
            public String sqlForGeneratedIntPKColumn(String columnName) {
                return columnName
                        + " IDENTITY ";
//                        " INTEGER " + " PRIMARY KEY " + " GENERATED ALWAYS AS IDENTITY ";
            }
            
            @Override
            public String sqlForConcat(String sqlForS1, String sqlForS2) {
                return sqlForS1 + " || " + sqlForS2;
            }
        },
        H2InMemory() {
            @Override
            public String getDriverFullClassName() {
                return "org.h2.Driver";
            }
            
            @Override
            public String getDriverType() {
                return "h2";
            }
            
            @Override
            public int getDefaultPort() {
                throw new Error("no port for H2 in local file mode : direct access to file system");
            }
            
            @Override
            public String getUrl(String hostName, int hostPort, String databaseName) {
                return "jdbc:h2:mem:" + databaseName;
            }
            
            @Override
            public String sqlForGeneratedIntPKColumn(String columnName) {
                return columnName
                        + " IDENTITY ";
//                        " INTEGER " + " PRIMARY KEY " + " GENERATED ALWAYS AS IDENTITY ";
            }
            
            @Override
            public String sqlForConcat(String sqlForS1, String sqlForS2) {
                return sqlForS1 + " || " + sqlForS2;
            }
        }
    }

    /**
     * permet le choix du type du SGBD par menu texte
     */
    public static Optional<SGBDConnus> menuTypeSGBD() {
        int rep = -1;
        SGBDConnus curS = null;
        int nbr = SGBDConnus.values().length;
        while (rep < 0 || rep > nbr) {
            System.out.println("choix du sgbd : ");
            for (int ns = 0; ns < SGBDConnus.values().length; ns++) {
                curS = SGBDConnus.values()[ns];
                System.out.println((ns + 1) + " : " + curS.toString());
            }
            System.out.println("0) Annuler");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
        }
        if (rep == 0) {
            return Optional.empty();
        } else {
            return Optional.of(curS);
        }
    }

    /**
     * permet le choix d'une connection soit spécifique soit générale par menu
     * texte
     */
    public static Optional<ConnectionSGBD> menuConnection() {
        int rep = -1;
        Connection con = null;
        boolean ok = false;
        ConnectionSGBD res = null;
        while (rep != 0 && !ok) {
            int i = 1;
            System.out.println("Menu de connection");
            System.out.println("==================");
            System.out.println((i++) + ") SGBDD H2 en mémoire database test");
            System.out.println((i++) + ") SGBD local postgres");
            System.out.println((i++) + ") SGBDD Mysql chez OVH");
            System.out.println((i++) + ") SGBDD Mysql chez freesqldatabase");
            System.out.println((i++) + ") autre SGBD");
            System.out.println("0) Annuler");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
            try {
                int j = 1;
                if (rep == j++) {
                    res = ConnectionSGBD.connect(ConnectionSGBD.SGBDConnus.H2InMemory,
                            "", 0, "test", "", "");
                    ok = true;
                } else if (rep == j++) {
                    String host = "92.222.25.165";
                    int port = 3306;
                    String user = ConsoleFdB.entreeString("nom de l'utilisateur");
                    String database = user;
                    System.out.println("host : " + host);
                    System.out.println("port : " + port);
                    System.out.println("database : " + database);
                    System.out.println("user : " + user);
                    String pass = ConsoleFdB.entreeString("entrez le mot de passe : ");
                    res = ConnectionSGBD.connect(ConnectionSGBD.SGBDConnus.MYSQL,
                            host, port, database, user, pass);
                    ok = true;
                } else if (rep == j++) {
                    String host = "sql7.freesqldatabase.com";
                    int port = 3306;
                    String database = "sql7555349";
                    String user = "sql7555349";
                    System.out.println("host : " + host);
                    System.out.println("port : " + port);
                    System.out.println("database : " + database);
                    System.out.println("user : " + user);
                    String pass = ConsoleFdB.entreeString("entrez le mot de passe : ");
                    res = ConnectionSGBD.connect(ConnectionSGBD.SGBDConnus.MYSQL,
                            host, port, database, user, pass);
                    ok = true;
                } else if (rep == j++) {
                    res = ConnectionSGBD.connect(ConnectionSGBD.SGBDConnus.POSTGRESQL,
                            "localhost", 5439, "postgres", "postgres", "pass");
                    ok = true;
                } else if (rep == j++) {
                    Optional<SGBDConnus> type = menuTypeSGBD();
                    if (type.isPresent()) {
                        if (type.get() == SGBDConnus.SQLITE) {
                            System.out.println("current path : " + new File("").getAbsolutePath());
                            String database = ConsoleFdB.entreeString("database path : ");
                            res = ConnectionSGBD.connect(type.get(),
                                    "", 0, database, "", "");
                            ok = true;
                        } else {
                            String host = ConsoleFdB.entreeString("host : ");
                            int port = ConsoleFdB.entreeInt("port : ");
                            String database = ConsoleFdB.entreeString("database : ");
                            String user = ConsoleFdB.entreeString("user : ");
                            String pass = ConsoleFdB.entreeString("mot de passe : ");
                            res = ConnectionSGBD.connect(type.get(),
                                    host, port, database, user, pass);
                            ok = true;
                        }
                    }
                }
                if (ok) {
                    System.out.println("connection OK");
                }
            } catch (SQLException ex) {
                System.out.println("Problem : " + ex.getLocalizedMessage());
            }
        }
        if (ok) {
            return Optional.of(res);
        } else {
            return Optional.empty();
        }
        
    }
    
    private SGBDDef sgbd;
    private Connection con;
    
    public ConnectionSGBD(SGBDDef sgbd, Connection con) {
        this.sgbd = sgbd;
        this.con = con;
    }
    
    public static ConnectionSGBD connect(SGBDDef sgbd,
            String host, int port,
            String database,
            String user, String pass) throws SQLException {
        return new ConnectionSGBD(sgbd, sgbd.connect(host, port, database, user, pass));
    }

    /**
     * @return the sgbd
     */
    public SGBDDef getSgbd() {
        return sgbd;
    }

    /**
     * @return the con
     */
    public Connection getCon() {
        return con;
    }
    
}
