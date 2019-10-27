package ru.thever4.yanboard;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;

/**
 * Created by thever4 on 11.03.19.
 */

public class FindServer {

    private static String server;

    public FindServer(boolean search) {
        this.server = null;
        if(search) {
            ArrayList<String> list = getLocalSubnets();
            if (list == null) return;
            String serv = getServer(getAvailableAddresses(list));
            if (serv != null) this.server = serv;
        }
    }

    public boolean checkAddress(String address) {
        ArrayList<String> list = new ArrayList<String>();
        list.add(address);
        if(getServer(list) != null) return true;
        return false;
    }

    public String getAddress() {
        return this.server;
    }

    private ArrayList<String> getLocalSubnets() {
        ArrayList<String> list = new ArrayList<String>();
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            while(nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration ias = ni.getInetAddresses();
                while(ias.hasMoreElements()) {
                    InetAddress ia = (InetAddress) ias.nextElement();
                    String enumAddr = ia.getHostAddress().toString();
                    if(enumAddr.contains("192.168.")) {
                        enumAddr = enumAddr.substring(0, enumAddr.lastIndexOf('.') + 1);
                        if(!(list.contains(enumAddr))) list.add(enumAddr);
                    }
                }
            }
        }
        catch (SocketException ex) {
            System.err.println("TODO Smth went wrong");
        }
        if(list.size() == 0) return null;
        return list;
    }

    private ArrayList<String> getAvailableAddresses(ArrayList<String> subnets) {
        ArrayList<String> list = new ArrayList<String>();
        int timeout = 30;
        for (int i = 0; i < subnets.size(); i++) {
            String net = subnets.get(i);
            for(int j = 1; j < 255; j++) {
                String host = net + Integer.toString(j);
                try{
                    if(InetAddress.getByName(host).isReachable(timeout)) list.add(host);
                }
                catch (Exception e) {
                    continue;
                }
            }
        }
        if (list.size() == 0) return null;
        return list;
    }

    /*private static String getServer(ArrayList<String> addresses) {
        Socket s;
        for(int i = 0; i < addresses.size(); i++) {
            String addr = addresses.get(i);
            try {
                s = new Socket(addr, 55765); //Вот это место крашит
                if(s.isConnected()) {
                    s.close();
                    return addr;
                }
            }
            catch (IOException e) {
                continue;
            }
        }
        s = null;
        return null;
    }
*/
    private String getServer(ArrayList<String> addresses) {
        try {
            this.server = new AsyncFinder().execute(addresses).get();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return this.server;
    }

    private static class AsyncFinder extends AsyncTask<ArrayList<String>, Integer, String> {

        @Override
        protected String doInBackground(ArrayList<String>... arrayLists) {
            Socket s;
            String addr;
            ArrayList<String> addresses = arrayLists[0];
            for(int i = 0; i < addresses.size(); i++) {
                addr = addresses.get(i);
                try {
                    s = new Socket(addr, 55765); //Вот это место крашит
                    if(s.isConnected()) {
                        s.close();
                        server = addr;
                        return addr;
                    }
                }
                catch (IOException e) {
                    continue;
                }
            }
            s = null;
            return null;
        }
    }



}
