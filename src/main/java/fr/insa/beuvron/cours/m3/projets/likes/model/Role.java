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

import fr.insa.beuvron.utils.database.ConnectionSGBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author francois
 */
public class Role {

    private int id;
    private String nom;
    private String description;

    public Role(int id, String nom, String description) {
        this.id = id;
        this.nom = nom;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Role{" + "id=" + id + ", nom=" + nom + ", description=" + description + '}';
    }

    public void sauvegarde(ConnectionSGBD connSGBD) throws SQLException {
        try (PreparedStatement st = connSGBD.getCon().prepareStatement(
                "insert into role (id,nom,description) values (?,?,?)")) {
            st.setInt(1, this.id);
            st.setString(2, this.nom);
            st.setString(3, this.description);
            st.executeUpdate();
        }
    }

    public static List<Role> tousLesRoles(ConnectionSGBD connSGBD) throws SQLException {
        List<Role> alls = new ArrayList<>();
        try (PreparedStatement st = connSGBD.getCon().prepareStatement(
                "select id,nom,description from role")) {
            ResultSet res = st.executeQuery();
            while (res.next()) {
                int id = res.getInt("id");
                String nom = res.getString("nom");
                String description = res.getString("description");
                alls.add(new Role(id, nom, description));
            }
        }
        return alls;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the nom
     */
    public String getNom() {
        return nom;
    }

    /**
     * @param nom the nom to set
     */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
