package org.samo_lego.simpleauth.storage;

import com.google.gson.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import static org.samo_lego.simpleauth.SimpleAuth.DB;
import static org.samo_lego.simpleauth.SimpleAuth.config;

/**
 * Class used for storing the non-authenticated player's cache
 */
public class PlayerCache {
    /**
     * Whether player is registered.
     */
    public boolean isRegistered;
    /**
     * Whether player is authenticated.
     * Used for {@link org.samo_lego.simpleauth.event.AuthEventHandler#onPlayerJoin(ServerPlayerEntity) session validation}.
     */
    public boolean isAuthenticated;
    /**
     * Hashed password of player.
     */
    public String password;
    /**
     * Stores how many times player has tried to login.
     */
    public int loginTries;
    /**
     * Last recorded IP of player.
     * Used for {@link org.samo_lego.simpleauth.event.AuthEventHandler#onPlayerJoin(ServerPlayerEntity) sessions}.
     */
    public String lastIp;
    /**
     * Time until session is valid.
     */
    public long validUntil;

    /**
     * Player stats before de-authentication.
     */
    public int lastAir;
    public boolean wasOnFire;
    public boolean wasInPortal;

    /**
     * Last recorded position before de-authentication.
     */
    public String lastDim;
    public double lastX;
    public double lastY;
    public double lastZ;

    private static final Gson gson = new Gson();

    public PlayerCache(String uuid, ServerPlayerEntity player) {
        if(DB.isClosed())
            return;

        if(player != null) {
            this.lastIp = player.getIp();

            this.lastAir = player.getAir();
            this.wasOnFire = player.isOnFire();

            // Setting position cache
            this.lastDim = String.valueOf(player.getEntityWorld().getRegistryKey().getValue());
            this.wasInPortal = player.getBlockState().getBlock().equals(Blocks.field_150427_aO);
            this.lastX = player.getX();
            this.lastY = player.getY();
            this.lastZ = player.getZ();
        }
        else {
            this.wasOnFire = false;
            this.wasInPortal = false;
            this.lastAir = 300;
        }

        if(DB.isUserRegistered(uuid)) {
            String data = DB.getData(uuid);

            // Getting (hashed) password
            JsonObject json = gson.fromJson(data, JsonObject.class);
            JsonElement passwordElement = json.get("password");
            if(passwordElement instanceof JsonNull) {
                if(player != null) {
                    player.sendMessage(new StringTextComponent(config.lang.corruptedPlayerData), false);
                }

                // This shouldn't have happened, data seems to be corrupted
                this.password = "";
                this.isRegistered = false;
            }
            else {
                this.password = passwordElement.getAsString();
                this.isRegistered = true;
            }


            // We should check the DB for saved coords
            if(config.main.spawnOnJoin) {
                try {
                    JsonElement lastLoc = json.get("lastLocation");
                    if (lastLoc != null) {
                        // Getting DB coords
                        JsonObject lastLocation = gson.fromJson(lastLoc.getAsString(), JsonObject.class);
                        this.lastDim = lastLocation.get("dim").isJsonNull() ? config.worldSpawn.dimension : lastLocation.get("dim").getAsString();
                        this.lastX = lastLocation.get("x").isJsonNull() ? config.worldSpawn.x : lastLocation.get("x").getAsDouble();
                        this.lastY = lastLocation.get("y").isJsonNull() ? config.worldSpawn.y : lastLocation.get("y").getAsDouble();
                        this.lastZ = lastLocation.get("z").isJsonNull() ? config.worldSpawn.z : lastLocation.get("z").getAsDouble();

                        // Removing location data from DB
                        json.remove("lastLocation");
                        DB.updateUserData(uuid, json.toString());
                    }
                } catch (JsonSyntaxException ignored) {
                    // Player didn't have any coords in db to tp to
                }
            }
        }
        else {
            this.isRegistered = false;
            this.password = "";
        }
        this.isAuthenticated = false;
        this.loginTries = 0;
    }
}
