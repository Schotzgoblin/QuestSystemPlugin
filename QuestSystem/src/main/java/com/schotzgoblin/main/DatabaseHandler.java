package com.schotzgoblin.main;

import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.Configuration;

import java.sql.*;
import java.util.logging.Level;

public class DatabaseHandler {
    private final SessionFactory sessionFactory;
    private final JavaPlugin plugin;

    public DatabaseHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        Configuration config = new Configuration();
        config.configure(); // Configure using hibernate.cfg.xml
        sessionFactory = config.buildSessionFactory();
//        sessionFactory = new MetadataSources(config.buildSessionFactory()).buildMetadata().buildSessionFactory();
    }

    private Session getSession() {
        return sessionFactory.openSession();
    }

    public void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
