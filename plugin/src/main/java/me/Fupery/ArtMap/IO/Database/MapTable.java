package me.Fupery.ArtMap.IO.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Canvas.CanvasSize;
import me.Fupery.ArtMap.IO.CompressedMap;
import me.Fupery.ArtMap.IO.MapId;

final class MapTable extends SQLiteTable {
    public MapTable(SQLiteDatabase database) {
        super(database, "maps", "CREATE TABLE IF NOT EXISTS maps (" +
                "id   INT   NOT NULL UNIQUE," +
                "hash INT   NOT NULL," +
                "map  BLOB  NOT NULL," +
                "resolution_factor INT NOT NULL DEFAULT 4," +
                "PRIMARY KEY (id)" +
                ");");
    }

    void migrate() throws SQLException {
        manager.getLock().lock();
        try (java.sql.Connection connection = manager.getConnection();
                Statement statement = connection.createStatement()) {
            try {
                statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN resolution_factor INT NOT NULL DEFAULT 4;");
            } catch (SQLException e) {
                if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("duplicate")) {
                    ArtMap.instance().getLogger().log(Level.FINE, "maps.resolution_factor migration skipped or applied", e);
                }
            }
        } finally {
            manager.getLock().unlock();
        }
    }

    void addMap(CompressedMap map) throws SQLException {
        new QueuedStatement() {
            @Override
			protected void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, map.getId());
                statement.setInt(2, map.getHash());
                statement.setBytes(3, map.getCompressedMap());
                statement.setInt(4, map.getResolutionFactor());
            }
        }.execute("INSERT INTO " + table + " (id, hash, map, resolution_factor) VALUES(?,?,?,?);");
    }

    void updateMapId(int oldMapId, int newMapId) throws SQLException {
        new QueuedStatement() {
            @Override
			protected void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, newMapId);
                statement.setInt(2, oldMapId);
            }
        }.execute("UPDATE " + table + " SET id=? WHERE id=?;");
    }

    Void deleteMap(int mapId) throws SQLException {
        return new QueuedStatement() {

            @Override
			protected void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, mapId);
            }
        }.execute("DELETE FROM " + table + " WHERE id=?;");
    }

    boolean containsMap(int mapId) throws SQLException {
        return new QueuedQuery<Boolean>() {
            @Override
            protected void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, mapId);
            }

            @Override
            protected Boolean read(ResultSet set) throws SQLException {
				return set.isBeforeFirst();
            }
        }.execute("SELECT hash FROM " + table + " WHERE id=?;");
    }

    void updateMap(CompressedMap map) throws SQLException {
        new QueuedStatement() {
            @Override
            protected void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, map.getHash());
                statement.setBytes(2, map.getCompressedMap());
                statement.setInt(3, map.getResolutionFactor());
                statement.setInt(4, map.getId());
            }
        }.execute("UPDATE " + table + " SET hash=?, map=?, resolution_factor=? WHERE id=?;");
    }

    Optional<CompressedMap> getMap(int mapId) throws SQLException {
        return new QueuedQuery<Optional<CompressedMap>>() {

            @Override
			protected void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, mapId);
            }

            @Override
			protected Optional<CompressedMap> read(ResultSet set) throws SQLException {
                if (!set.next()) return Optional.empty();
                int id = set.getInt("id");
                int hash = set.getInt("hash");
                byte[] map = set.getBytes("map");
                int factor = readResolutionFactor(set);
                return Optional.of(new CompressedMap(id, hash, map, factor));
            }
        }.execute("SELECT * FROM " + table + " WHERE id=?;");
    }

    Integer getHash(int mapId) throws SQLException {
        return new QueuedQuery<Integer>() {

            @Override
			protected void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, mapId);
            }

            @Override
			protected Integer read(ResultSet set) throws SQLException {
                return (set.next()) ? set.getInt("hash") : null;
            }
        }.execute("SELECT hash FROM " + table + " WHERE id=?;");
    }

    Optional<Integer> getResolutionFactor(int mapId) throws SQLException {
        return new QueuedQuery<Optional<Integer>>() {
            @Override
            protected void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, mapId);
            }

            @Override
            protected Optional<Integer> read(ResultSet set) throws SQLException {
                if (!set.next()) {
                    return Optional.empty();
                }
                return Optional.of(readResolutionFactor(set));
            }
        }.execute("SELECT resolution_factor FROM " + table + " WHERE id=?;");
    }

    List<MapId> getMapIds() throws SQLException {
        return new QueuedQuery<List<MapId>>() {

            @Override
			protected void prepare(PreparedStatement statement) {
                //not needed
            }

            @Override
			protected List<MapId> read(ResultSet set) throws SQLException {
                List<MapId> mapHashes = new ArrayList<>(set.getFetchSize());
                while (set.next()) {
                    mapHashes.add(new MapId(set.getInt("id"), set.getInt("hash")));
                }
                return mapHashes;
            }
        }.execute("SELECT id, hash FROM " + table + ";");
    }

    List<CompressedMap> addMaps(List<CompressedMap> maps) throws SQLException {
        List<CompressedMap> failed = new ArrayList<>();
        new QueuedStatement() {
            @Override
            protected void prepare(PreparedStatement statement) throws SQLException {
                for (CompressedMap map : maps) {
                    try {
                        statement.setInt(1, map.getId());
                        statement.setInt(2, map.getHash());
                        statement.setBytes(3, map.getCompressedMap());
                        statement.setInt(4, map.getResolutionFactor());
                    } catch (Exception e) {
                        failed.add(map);
                        ArtMap.instance().getLogger().log(Level.SEVERE, String.format("Error writing map %s to database!", map.getId()),e);
                        continue;
                    }
                    statement.addBatch();
                }
            }
        }.executeBatch("INSERT INTO " + table + " (id, hash, map, resolution_factor) VALUES(?,?,?,?);");
        return failed;
    }

    private static int readResolutionFactor(ResultSet set) throws SQLException {
        try {
            return set.getInt("resolution_factor");
        } catch (SQLException e) {
            return CanvasSize.NORMAL.getResolutionFactor();
        }
    }
}
