package net.Indyuce.mmoitems.manager.data;

import io.lumine.mythic.lib.data.DefaultOfflineDataHolder;
import io.lumine.mythic.lib.data.queue.DataLoadResult;
import io.lumine.mythic.lib.data.sql.SQLDatabase;
import io.lumine.mythic.lib.data.sql.UpdateRequestBuilder;
import io.lumine.mythic.lib.gson.JsonParser;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SQLDatabaseImpl extends SQLDatabase<PlayerData, DefaultOfflineDataHolder> {
    public SQLDatabaseImpl() {
        super(MMOItems.plugin, UUID_FIELD_NAME);
    }

    private static final String UUID_FIELD_NAME = "uuid";

    @Override
    protected void setupSQL() throws SQLException {

        // Create table if not exists
        executeUpdate("CREATE TABLE IF NOT EXISTS `" + userdataTableName + "` (" +
                UUID_FIELD_NAME + " VARCHAR(36) NOT NULL," +
                "json LONGTEXT," +
                "is_saved TINYINT," +
                "PRIMARY KEY (uuid));");
    }

    @NotNull
    @Override
    protected DataLoadResult loadDataFromResultSet(@NotNull PlayerData playerData, @NotNull ResultSet resultSet, boolean force) throws SQLException {

        var json = resultSet.getString("json");
        if (json != null && !json.isEmpty()) {
            playerData.loadFromJson(JsonParser.parseString(json));
            return new DataLoadResult(false, force);
        }

        return new DataLoadResult(true, force);
    }

    @Override
    protected void setupSaveRequest(@NotNull PlayerData playerData, @NotNull UpdateRequestBuilder<PlayerData> builder) {
        builder.appendString("json", playerData.toJson().toString());
    }

    @Override
    public DefaultOfflineDataHolder getOffline(@NotNull UUID uuid) {
        return new DefaultOfflineDataHolder(uuid);
    }
}
