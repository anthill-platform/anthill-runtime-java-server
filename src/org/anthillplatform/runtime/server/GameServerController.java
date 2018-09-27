package org.anthillplatform.runtime.server;

import org.anthillplatform.runtime.services.GameService;
import org.anthillplatform.runtime.services.LoginService;
import org.anthillplatform.runtime.util.JsonRPC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;


/**
 * Game Server Controller
 *
 * This class manages communication with the Controller Service using ZeroMQ library for transport-level operations,
 * and JSON-RPC protocol for actual requests made to the Controller Service.
 *
 * @author desertkun
 */

public abstract class GameServerController
{
    private final ZMQ.Socket socket;
    private final ZContext context;
    private JsonRPC rpc;

    public interface InitedHandler
    {
        void result(boolean success);
    }

    public interface PlayerJoinedHandler
    {
        void result(boolean success, LoginService.AccessToken token,
                    String account, String credential,
                    LoginService.Scopes scopes, JSONObject info);
    }

    public interface PlayerLeftHandler
    {
        void result(boolean success);
    }

    public interface UpdateSettingsHandler
    {
        void result(boolean success);
    }

    public interface CheckDeploymentHandler
    {
        void result(boolean success);
    }

    @SuppressWarnings("WeakerAccess")
    protected abstract String getStatus();

    @SuppressWarnings("WeakerAccess")
    protected void logInfo(String log)
    {
        System.out.println(log);
    }

    @SuppressWarnings("WeakerAccess")
    protected void logError(String log)
    {
        System.err.println(log);
    }

    public GameServerController(String socket)
    {
        logInfo("Listening from Controller Service on " + socket);

        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.PAIR);
        this.socket.connect("ipc://" + socket);

        this.rpc = new JsonRPC()
        {
            @Override
            protected void send(String data)
            {
                GameServerController.this.socket.send(data, ZMQ.NOBLOCK);
            }

            @Override
            public void onError(int code, String message, String data)
            {
                logError("Error: " + code + " " + message + " " + data);
            }
        };
    }

    /**
     * The main update cycle method. Need to be called in the main process loop
     */
    public void update()
    {
        String data;

        while ((data = socket.recvStr(ZMQ.NOBLOCK)) != null)
        {
            received(data);
        }
    }

    public void stop()
    {
        context.destroy();
    }

    public JsonRPC getRpc()
    {
        return rpc;
    }

    private void received(String data)
    {
        getRpc().received(data);
    }

    public void joined(String key, PlayerJoinedHandler handler)
    {
        joined(key, null, null, handler);
    }

    /**
     * Performs 'Player Joined Request'
     *
     * See https://github.com/anthill-services/anthill-game/blob/master/doc/API.md#player-joined-request
     *
     * @param key The registration Key
     * @param extendToken If both extend_token and extend_scopes are passed diring the joined request, the Access Token
     *                    of the player will be extended using extend_token as master token and extend_scopes as a
     *                    list of scopes the Player's Access Token should be extended with.
     * @param extendScopes see extendToken
     * @param handler Callback with call response
     */
    public void joined(String key, LoginService.AccessToken extendToken, String extendScopes,
        final PlayerJoinedHandler handler)
    {
        final LoginService loginService = LoginService.Get();

        if (loginService == null)
            throw new RuntimeException("No login service!");

        JSONObject params = new JSONObject();

        params.put("key", key);

        if (extendToken != null && extendScopes != null)
        {
            params.put("extend_token", extendToken.get());
            params.put("extend_scopes", extendScopes);
        }

        getRpc().request("joined", new JsonRPC.ResponseHandler()
        {
            @Override
            public void success(Object response)
            {
                String raw = ((JSONObject) response).getString("access_token");

                JSONArray scopes_ = ((JSONObject) response).optJSONArray("scopes");
                LoginService.Scopes scopes = new LoginService.Scopes();

                for (int i = 0, t = scopes_.length(); i < t; i++)
                {
                    scopes.add(scopes_.getString(i));
                }

                String account = ((JSONObject) response).optString("account");
                String credential = ((JSONObject) response).optString("credential");

                JSONObject info = ((JSONObject) response).optJSONObject("info");
                handler.result(true, loginService.newAccessToken(raw), account, credential, scopes, info);
            }

            @Override
            public void error(int code, String message, String data)
            {
                logError("Error while joining: " + code + " " + message + " " + data);
                handler.result(false, null, null, null, null, null);
            }
        }, params);
    }

    /**
     * Performs 'Player Left Request'
     *
     * See https://github.com/anthill-services/anthill-game/blob/master/doc/API.md#player-left-request
     *
     * @param key The registration Key
     * @param handler Callback with call response
     */
    public void left(String key, final PlayerLeftHandler handler)
    {
        JSONObject params = new JSONObject();

        params.put("key", key);

        getRpc().request("left", new JsonRPC.ResponseHandler()
        {
            @Override
            public void success(Object response)
            {
                handler.result(true);
            }

            @Override
            public void error(int code, String message, String data)
            {
                logError("Error to leave the player: " + code + " " + message + " " + data);
                handler.result(false);
            }
        }, params);
    }

    /**
     * Performs 'Update Room Settings Request'
     *
     * See https://github.com/anthill-services/anthill-game/blob/master/doc/API.md#update-room-settings-request
     *
     * @param settings New settings for the Room
     * @param handler Callback with call response
     */
    public void updateSettings(GameService.RoomSettings settings, final UpdateSettingsHandler handler)
    {
        JSONObject params = new JSONObject();

        params.put("settings", settings.getSettings());

        getRpc().request("update_settings", new JsonRPC.ResponseHandler()
        {
            @Override
            public void success(Object response)
            {
                handler.result(true);
            }

            @Override
            public void error(int code, String message, String data)
            {
                logError("Error to update settings: " + code + " " + message + " " + data);
                handler.result(false);
            }
        }, params);
    }

    /**
     * Performs 'Check Game Server Deployment Request'
     *
     * See https://github.com/anthill-services/anthill-game/blob/master/doc/API.md#check-game-server-deployment-request
     *
     * @param handler Callback with call response
     */
    public void checkDeployment(final CheckDeploymentHandler handler)
    {
        JSONObject params = new JSONObject();

        getRpc().request("check_deployment", new JsonRPC.ResponseHandler()
        {
            @Override
            public void success(Object response)
            {
                logError("Deployment is up to date.");
                handler.result(true);
            }

            @Override
            public void error(int code, String message, String data)
            {
                logError("Deployment check failed: " + code + " " + message + " " + data);
                handler.result(false);
            }
        }, params);
    }

    public void inited(final InitedHandler handler)
    {
        inited(null, handler);
    }

    /**
     * Performs 'Initialized Request'
     *
     * See https://github.com/anthill-services/anthill-game/blob/master/doc/API.md#initialized-request
     *
     * @param settings (Optional) Update room settings along with initialization
     * @param handler Callback with call response
     */
    public void inited(GameService.RoomSettings settings, final InitedHandler handler)
    {
        getRpc().addHandler("status", new JsonRPC.MethodHandler()
        {
            @Override
            public Object called(Object params) throws JsonRPC.JsonRPCException
            {
                JSONObject status = new JSONObject();
                status.put("status", getStatus());
                return status;
            }
        });

        JSONObject params = new JSONObject();

        if (settings != null)
        {
            params.put("settings", settings.getSettings());
        }

        getRpc().request("inited", new JsonRPC.ResponseHandler()
        {
            @Override
            public void success(Object response)
            {
                logError("Inited: " + response.toString());
                handler.result(true);
            }

            @Override
            public void error(int code, String message, String data)
            {
                logError("Error while inited: " + code + " " + message + " " + data);
                handler.result(false);
            }
        }, params);
    }
}
