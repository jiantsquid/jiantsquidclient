package org.jiantsquid.jiantsquidclient;

import java.io.IOException;

import org.jiantsquid.network.p2p.message.Request;
import org.jiantsquid.network.p2p.message.Response;
import org.jiantsquid.network.protocol.Connection;

public class ClientApi {

	private Connection connection ;
	
	public ClientApi( String host, int port ) throws IOException{
		try {
			Thread.sleep( 2000 ) ;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		connection = new Connection( host, port ) ;
		connection.connect(); 
	}
	
	public Response send( Request request ) throws IOException {
		return connection.sendRequest( request ) ;
	}
	
	public void close( ) throws IOException {
		connection.close() ; 
	}
}
