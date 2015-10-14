/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.vcard4android;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;

import lombok.NonNull;

public class BatchOperation {
	private final ContentProviderClient providerClient;
	private final ArrayList<ContentProviderOperation> queue = new ArrayList<>();
	ContentProviderResult[] results;


	public BatchOperation(@NonNull ContentProviderClient providerClient) {
		this.providerClient = providerClient;
	}

	public int nextBackrefIdx() {
		return queue.size();
	}

	public void enqueue(ContentProviderOperation operation) {
		queue.add(operation);
	}

	public int commit() throws ContactsStorageException {
		int affected = 0;
		if (!queue.isEmpty())
			try {
				Constants.log.debug("Committing " + queue.size() + " operations …");
				results = providerClient.applyBatch(queue);
				for (ContentProviderResult result : results)
					if (result != null)                 // will have either .uri or .count set
						if (result.count != null)
							affected += result.count;
						else if (result.uri != null)
							affected += 1;
				Constants.log.debug("… " + affected + " record(s) affected");
			} catch(OperationApplicationException|RemoteException e) {
				throw new ContactsStorageException("Couldn't apply batch operation", e);
			}
		queue.clear();
		return affected;
	}

	public ContentProviderResult getResult(int idx) {
		return results[idx];
	}
}
