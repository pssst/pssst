// Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.
package name.pssst.gui.internal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import name.pssst.api.exception.ApiException;
import name.pssst.app.R;

/**
 * Abstract background task.
 * @author Christian & Christian
 */
public abstract class TaskBase<Param, Result> extends AsyncTask<Param, Void, Object> {
    protected Context context;

    /**
     * Saves activity context.
     * @param context context
     */
    protected TaskBase(Context context) {
        this.context = context;
    }

    /**
     * Executes onExecute.
     */
    @Override
    protected void onPreExecute() {
        onExecute();
    }

    /**
     * Method stub.
     */
    protected void onExecute() {}

    /**
     * Executes doExecute.
     * @param param parameter
     */
    @Override
    protected Object doInBackground(Param... param) {
        try {
            return doExecute(param);
        } catch (Exception e) {
            return e;
        }
    }

    /**
     * Should execute a background task.
     * @param param parameter
     * @return result
     * @throws Exception
     */
    protected abstract Result doExecute(Param... param) throws Exception;

    /**
     * Executes onFinished or shows exception.
     * @param result result or exception
     */
    @SuppressWarnings({"unchecked"})
    @Override
    protected void onPostExecute(Object result) {
        if (result instanceof Exception) {
            showDialog(((ApiException) result).getMessage());
            onFinished(true, null);
        } else {
            onFinished(false, (Result)result);
        }
    }

    /**
     * Method stub.
     * @param error error
     * @param result result
     */
    protected void onFinished(boolean error, Result result) {}

    /**
     * Shows a standard notice
     * @param message message
     */
    protected void showNotice(String message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows a standard dialog.
     * @param message message
     */
    protected void showDialog(String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        String positive = context.getResources().getString(R.string.button_positive);

        alert.setMessage(message);
        alert.setPositiveButton(positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.cancel();
            }
        });
        alert.show();
    }
}
