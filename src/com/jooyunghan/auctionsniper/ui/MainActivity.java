package com.jooyunghan.auctionsniper.ui;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.jooyunghan.auctionsniper.Auction;
import com.jooyunghan.auctionsniper.AuctionSniper;
import com.jooyunghan.auctionsniper.R;
import com.jooyunghan.auctionsniper.SniperSnapshot;
import com.jooyunghan.auctionsniper.UIThreadSniperListener;
import com.jooyunghan.auctionsniper.UserRequestListener;
import com.jooyunghan.auctionsniper.R.id;
import com.jooyunghan.auctionsniper.R.layout;
import com.jooyunghan.auctionsniper.R.menu;
import com.jooyunghan.auctionsniper.xmpp.XMPPAuction;
import com.jooyunghan.util.Announcer;

public class MainActivity extends Activity {
	private static final int ARG_HOSTNAME = 0;
	private static final int ARG_USERNAME = 1;
	private static final int ARG_PASSWORD = 2;
	private static final String AUCTION_RESOURCE = "Auction";

	private final Announcer<UserRequestListener> userRequests = Announcer
			.to(UserRequestListener.class);
	private List<Auction> notToBeGCd = new ArrayList<Auction>();
	private SnipersAdapter snipers;
	public XMPPConnection connection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ListView list = (ListView) findViewById(R.id.list);
		snipers = new SnipersAdapter(this);
		list.setAdapter(snipers);

		final TextView itemIdField = (TextView) findViewById(R.id.item_id_text);
		final Button button = (Button) findViewById(R.id.bid_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				userRequests.announce().joinAuction(
						itemIdField.getText().toString());
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		// if (connection != null && connection.isConnected()) {
		// connection.disconnect();
		// connection = null;
		// Log.d("han", "disconnected");
		// }
	}

	public static XMPPConnection connectTo(String host, String username,
			String password) throws XMPPException {
		ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(
				host, 5222);
		XMPPConnection connection = new XMPPConnection(connectionConfiguration);
		connection.connect();
		Log.d("han", "connect:" + username);
		connection.login(username, password, AUCTION_RESOURCE);
		Log.d("han", "login:" + username);
		return connection;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_join) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// test-only method (called from test)
	public void main(final String[] args) throws XMPPException {
		connection = connectTo(args[ARG_HOSTNAME], args[ARG_USERNAME],
				args[ARG_PASSWORD]);
		addUserRequestListener(new UserRequestListener() {
			@Override
			public void joinAuction(String itemId) {
				snipers.addSniper(SniperSnapshot.joining(itemId));
				Auction auction = new XMPPAuction(connection, itemId);
				notToBeGCd.add(auction);
				auction.addAuctionEventListener(new AuctionSniper(itemId,
						auction, new UIThreadSniperListener(MainActivity.this,
								snipers)));
				auction.join();
			}
		});
	}

	public void addUserRequestListener(UserRequestListener userRequestListener) {
		userRequests.addListener(userRequestListener);
	}
}