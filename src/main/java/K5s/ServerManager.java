package K5s;

import K5s.connectionManager.ServerMessageThread;
import K5s.storage.Server;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static K5s.protocol.GossipMessages.gossipMessage;
import static K5s.protocol.LeaderProtocol.*;

public class ServerManager {
    private static ServerManager instance;
    private static ChatServer meServer;

    private ServerManager(ChatServer meServer){
        this.meServer=meServer;
    }

    /**
     * returns true if the identity might get approved else false
     * @param identity
     * @return
     */
    public static String  isAvailableIdentity(String identity){
        if(meServer.getGlobalIdentity().contains(identity)){
            return "FALSE";
        }else {
            System.out.println("Leader : "+meServer.getLeader());
            if (!meServer.isLeader()){
                JSONObject newtIdentityApprovalRequest = newtIdentityApprovalRequest(meServer.getServerId(),identity);
                if(!meServer.getLeader().isEmpty()){
                    try {
                        send(newtIdentityApprovalRequest,meServer.getLeader());
                        return "WAITING";
                    } catch (IOException e) {
                        e.printStackTrace();
                        return "FALSE";
                        //this it just an availability measure
                    }
                }else {
//                    TODO:electionmessage
                }
            }else {
                return "TRUE";
            }
        }
        return "FALSE";
    }

    public static ServerManager getInstance(ChatServer meserver){
        if (instance==null){
            instance=new ServerManager(meserver);
        }
        return instance;
    }

    public void initiateLeaderElection(){
        meServer.setElectionInProgress(true);
        meServer.setisOkMessageReceived(false);
        ArrayList<Server> otherServers = meServer.getOtherServers();
        int count = 0;
        for(Server s : otherServers){
            if (s.getServerId().compareTo(meServer.getServerId()) > 0){
                try{
                    send(electionMessage(meServer.getServerId()), s.getServerId());
                    count += 1;
                }catch(IOException e){
                    System.out.println("ELECTION Server "+s.getServerId()+ "is down");
                }
            }
        }

        if(count == 0){

            for (Server s: otherServers) {
                try{
                    send(coordinatorMessage(meServer.getServerId()), s.getServerId());
                } catch(IOException e) {
                    System.out.println("COORDINATOR Server "+s.getServerId()+ " is down");
                }
            }

            meServer.setLeader(meServer.getServerId());
            meServer.setElectionInProgress(false);
            System.out.println(meServer.getServerId()+ " is Leader");
        }
//        timer.schedule(task, 4000);
    }
    private void gossipState(String serverId) throws IOException {
        send(gossipMessage(meServer.getState() ,meServer.getOtherServerIdJSONArray()),serverId);
    }

    public static ChatServer getMeServer() {
        return meServer;
    }

    private static void send(JSONObject obj, String serverId) throws IOException {
        Server server=meServer.getServer(serverId);
        if (server!=null){
            Socket ss=server.getSocket();
            System.out.println(ss.getPort());
            OutputStream out = ss.getOutputStream();
            out.write((obj.toString() + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }
        System.out.println("Reply :" + obj );

//        out.flush();
//        out.close();
    }
}
