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

package de.nextbill.client.breceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.network.RequestManager;

public class RequestServiceBroadcastReceiver extends BroadcastReceiver {
	private RequestListener listener;

	public RequestServiceBroadcastReceiver() { }
	
	public RequestServiceBroadcastReceiver(RequestListener listener) {
		this.listener=listener;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();

		switch (action) {
			case RequestManager.BC_REQUEST_DONE:
				if (listener != null) {
					String activityClassName = intent.getStringExtra("ACTIVITY_CLASS_NAME");
					listener.requestDone(activityClassName);
				}
				break;
			case RequestManager.BC_LOGIN:
				if (listener != null)
					listener.loginMessage(LoginStatusEnum.valueOf(intent.getStringExtra("LOGIN_STATUS_ENUM")));
				break;
			case RequestManager.BC_ONLINE:
				if (listener != null)
					listener.onlineStatusMessage(OnlineStatusEnum.valueOf(intent.getStringExtra("ONLINE_STATUS_ENUM")));
				break;
			case RequestManager.BC_SEND_MESSAGE:
				if (listener != null)
					listener.generalMessage(intent.getStringExtra("ACTIVITY_CLASS_NAME"), BroadcastMessageActionEnum.valueOf(intent.getStringExtra("ACTION")), intent.getStringExtra("PROGRESS_MESSAGE"));
				break;
		}
	}
}
