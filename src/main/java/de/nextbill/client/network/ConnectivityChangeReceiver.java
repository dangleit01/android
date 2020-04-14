/*
 * NextBill Android client application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.client.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityChangeReceiver extends BroadcastReceiver
{
	
	public static final String TAG="ConnectivityChangeReceiver";
	private ConnectionChangeListener listener; 
	
	public ConnectivityChangeReceiver(Context context)
	{
		listener=null;
		if(context instanceof ConnectionChangeListener)
			listener=(ConnectionChangeListener)context;
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info1 = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		//TODO TEST
		NetworkInfo info2 = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		boolean isConnected1 = info1!=null && info1.isConnected();
		boolean isConnected2 = info2!=null && info2.isConnected();
		boolean isConnected=(isConnected1|isConnected2);
		if(isConnected)
		{
			if(listener!=null)
				if (isConnected2){
					listener.onConnectionChange(NetworkUtils.TYPE_MOBILE);
				}else if (isConnected1){
					listener.onConnectionChange(NetworkUtils.TYPE_WIFI);
				}

		}
		else
		{
			if(listener!=null)
				listener.onConnectionChange(NetworkUtils.TYPE_NOT_CONNECTED);
		}
	}

	public interface ConnectionChangeListener
	{
		public void onConnectionChange(Integer connectionMode);
	}
}
