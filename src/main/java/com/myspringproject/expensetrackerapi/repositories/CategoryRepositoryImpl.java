package com.myspringproject.expensetrackerapi.repositories;

import com.myspringproject.expensetrackerapi.domain.Category;
import com.myspringproject.expensetrackerapi.exceptions.EtBadRequestException;
import com.myspringproject.expensetrackerapi.exceptions.EtResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    @Autowired
    JdbcTemplate jdbcTemplate;

    private RowMapper<Category> categoryRowMapper = ((rs, rowNum) -> {
        return new Category(rs.getInt("CATEGORY_ID"),
                rs.getInt("USER_ID"),
                rs.getString("TITLE"),
                rs.getString("DESCRIPTION"),
                rs.getDouble("TOTAL_EXPENSE"));
    });

    private static final String SQL_UPDATE_CATEGORY = "UPDATE ET_CATEGORIES SET TITLE = ? ," +
            "DESCRIPTION = ? WHERE USER_ID = ? AND CATEGORY_ID = ?";

    private static final String SQL_FIND_ALL = "SELECT C.CATEGORY_ID, C.USER_ID, C.TITLE, C.DESCRIPTION, " +
            "COALESCE(SUM(T.AMOUNT), 0) TOTAL_EXPENSE " +
            "FROM ET_TRANSACTIONS T RIGHT OUTER JOIN ET_CATEGORIES C on C.CATEGORY_ID = T.CATEGORY_ID " +
            "WHERE C.USER_ID = ? GROUP BY C.CATEGORY_ID";

    private static final String SQL_FIND_BY_ID = "SELECT C.CATEGORY_ID, C.USER_ID, C.TITLE, C.DESCRIPTION, " +
            "COALESCE(SUM(T.AMOUNT), 0) TOTAL_EXPENSE " +
            "FROM ET_TRANSACTIONS T RIGHT OUTER JOIN ET_CATEGORIES C on C.CATEGORY_ID = T.CATEGORY_ID " +
            "WHERE C.USER_ID = ? AND C.CATEGORY_ID = ? GROUP BY C.CATEGORY_ID";

    private static final String SQL_CREATE_CATEGORY = "INSERT INTO ET_CATEGORIES (CATEGORY_ID, USER_ID, TITLE, DESCRIPTION) " +
            "VALUES(NEXTVAL('ET_CATEGORIES_SEQ'), ?, ?, ?)";

    private static final String SQL_DELETE_CATEGORY = "DELETE FROM ET_CATEGORIES WHERE USER_ID = ? AND CATEGORY_ID = ?";

    private static final String SQL_DELETE_ALL_TRANSACTIONS = "DELETE FROM ET_TRANSACTIONS WHERE CATEGORY_ID = ?";



    @Override
    public List<Category> fetchAll(Integer userId) throws EtResourceNotFoundException {
       try {
           return jdbcTemplate.query(SQL_FIND_ALL, new Object[]{userId}, categoryRowMapper);
       } catch (Exception e) {
           throw new EtResourceNotFoundException("Invalid UserId");
       }
    }

    @Override
    public Category findById(Integer userId, Integer categoryId) throws EtResourceNotFoundException {
        try {
           return jdbcTemplate.queryForObject(SQL_FIND_BY_ID, new Object[]{userId, categoryId}, categoryRowMapper);
        } catch (Exception e) {
            throw new EtResourceNotFoundException("Category not found");
        }
    }

    @Override
    public Integer create(Integer userId, String title, String description) throws EtBadRequestException {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(SQL_CREATE_CATEGORY, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, userId);
                ps.setString(2, title);
                ps.setString(3, description);
                return ps;
            }, keyHolder);
            return (Integer) keyHolder.getKeys().get("CATEGORY_ID");
        } catch (Exception e) {
            throw new EtBadRequestException("Invalid Request");
        }

    }

    @Override
    public void update(Integer userId, Integer categoryId, Category category) {

        try {
            jdbcTemplate.update(SQL_UPDATE_CATEGORY, new Object[]{category.getTitle(), category.getDescription(), userId, categoryId});
        } catch (Exception e) {
            throw new EtBadRequestException("Invalid Request");
        }
    }

    @Override
    public void removeId(Integer userId, Integer categoryId) {
        try {
            this.removeCatTransaction(categoryId);
            jdbcTemplate.update(SQL_DELETE_CATEGORY, new Object[]{userId, categoryId});
        } catch (Exception e) {
            throw new EtBadRequestException("Invalid request for delete category");
        }
    }

    private void removeCatTransaction(Integer categoryId) {
        try {
            jdbcTemplate.update(SQL_DELETE_ALL_TRANSACTIONS, new Object[]{categoryId});
        } catch (Exception e) {
            throw new EtBadRequestException("Invalid request for delete transactions of a category");
        }

    }
}
