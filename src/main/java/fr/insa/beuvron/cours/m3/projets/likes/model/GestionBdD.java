/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.beuvron.cours.m3.projets.likes.model;

import fr.insa.beuvron.utils.ConsoleFdB;
import fr.insa.beuvron.utils.database.ConnectionSGBD;
import fr.insa.beuvron.utils.exceptions.ExceptionsUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author francois
 */
public class GestionBdD {

//    public static Connection connectionMariaDB(String host, int port, String database,
//            String user, String pass) throws SQLException {
//        Connection con = DriverManager.getConnection(
//                // url = jdbc:<nom du gestionnaire>://<adresse internet>:<port>/<nom de la base>
//                "jdbc:" + "mariadb://" + host + ":" + port + "/" + database,
//                user, pass);
//        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
//        return con;
//    }

    public static ConnectionSGBD defautCon() throws SQLException {
        return ConnectionSGBD.connect(ConnectionSGBD.SGBDConnus.MARIADB,
                "localhost",3306,"test","test","pass");
    }

    public static void creeSchema(ConnectionSGBD connSGBD) throws SQLException {
        Connection conn = connSGBD.getCon();
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "create table role (\n"
                    + "  id integer primary key,\n"
                    + "  nom varchar(20),\n"
                    + "  description text \n"
                    + ")");
            st.executeUpdate(
                    "create table utilisateur (\n"
                    + connSGBD.getSgbd().sqlForGeneratedIntPKColumn("id") + ",\n"
                    + "  login varchar(50),\n"
                    + "  password varchar(40),\n"
                    + "  description text, \n"
                    + "  idrole integer"
                    + ")");
            st.executeUpdate(
                    "create table apprecie (\n"
                    + "  u1 integer,\n"
                    + "  u2 integer"
                    + ")");
            st.executeUpdate(
                    "alter table utilisateur \n"
                    + "  add constraint fk_utilisateur_idrole \n"
                    + "  foreign key (idrole) references role(id)");
            st.executeUpdate(
                    "alter table apprecie \n"
                    + "  add constraint fk_apprecie_u1 \n"
                    + "  foreign key (u1) references utilisateur(id)");
            st.executeUpdate(
                    "alter table apprecie \n"
                    + "  add constraint fk_apprecie_u2 \n"
                    + "  foreign key (u2) references utilisateur(id)");
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static void initialise(ConnectionSGBD connSGBD) throws SQLException {
        Connection conn = connSGBD.getCon();
        Role radmin = new Role(1, "admin", "administrateur");
        radmin.sauvegarde(connSGBD);
        Role user = new Role(2, "user", "utilisateur de base");
        user.sauvegarde(connSGBD);
        Utilisateur admin = new Utilisateur("admin", "admin", "administrateur du site", 1);
        admin.sauvegarde(connSGBD);
    }
    
    public static void razBdD(ConnectionSGBD connSGBD) throws SQLException {
        supprimeSchema(connSGBD);
        creeSchema(connSGBD);
        initialise(connSGBD);
    }

    public static void supprimeSchema(ConnectionSGBD connSGBD) throws SQLException {
        Connection conn = connSGBD.getCon();
        try (Statement st = conn.createStatement()) {
            try {
                st.executeUpdate("alter table utilisateur drop constraint fk_utilisateur_idrole");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("alter table apprecie drop constraint fk_apprecie_u1");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("alter table apprecie drop constraint fk_apprecie_u2");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table apprecie");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table utilisateur");
            } catch (SQLException ex) {
            }
            try {
                st.executeUpdate("drop table role");
            } catch (SQLException ex) {
            }
        }

    }

    public static void menuPrincipal(ConnectionSGBD connSGBD) {
        int rep = -1;
        while (rep != 0) {
            int i = 1;
            System.out.println("Menu principal");
            System.out.println("==============");
            System.out.println((i++) + ") supprimer schéma");
            System.out.println((i++) + ") créer schéma");
            System.out.println((i++) + ") initialiser la BdD");
            System.out.println((i++) + ") RAZ de la BdD =  supprime + crée + init");
            System.out.println((i++) + ") menu utilisateur");
            System.out.println("0) Fin");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
            try {
                int j = 1;
                if (rep == j++) {
                    supprimeSchema(connSGBD);
                } else if (rep == j++) {
                    creeSchema(connSGBD);
                } else if (rep == j++) {
                    initialise(connSGBD);
                } else if (rep == j++) {
                    razBdD(connSGBD);
                } else if (rep == j++) {
                    Utilisateur.menuUtilisateur(connSGBD);
                }
            } catch (SQLException ex) {
                System.out.println(ExceptionsUtils.messageEtPremiersAppelsDansPackage(ex, "fr.insa", 5));
            }
        }
    }

    public static void debut() {
        try {
            ConnectionSGBD connSGBD = defautCon();
            System.out.println("Connecte !!");
            menuPrincipal(connSGBD);
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }

    public static void main(String[] args) {
        debut();
    }

}
