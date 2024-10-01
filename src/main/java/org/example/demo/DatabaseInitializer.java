package org.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        int userUpdated = jdbcTemplate.update("UPDATE user SET text = ? WHERE id = ?", null, 1);
        int subUpdated = jdbcTemplate.update("DELETE FROM sub");
        System.out.println(String.format("initial user:%s, sub:%s", userUpdated, subUpdated));
    }
}