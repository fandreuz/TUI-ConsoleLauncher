package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by mhashim6 on 04/06/2017.
 */

public class keepn implements CommandAbstraction {
	@Override
	public String exec(ExecutePack pack) throws Exception {

		MainPack info = (MainPack) pack;

		String noteToKeep = info.get(String.class, 0);
		return addToKeep(noteToKeep, info);
	}

	@Override
	public int minArgs() {
		return 1;
	}

	@Override
	public int maxArgs() {
		return CommandAbstraction.UNDEFINIED;
	}

	@Override
	public int[] argType() {
		return new int[]{CommandAbstraction.PLAIN_TEXT};
	}

	@Override
	public int priority() {
		return 2;
	}

	@Override
	public int helpRes() {
		return R.string.help_keepn;
	}

	@Override
	public String onArgNotFound(ExecutePack pack) {
		MainPack info = (MainPack) pack;
		return info.res.getString(R.string.help_keepn);
	}

	@Override
	public String onNotArgEnough(ExecutePack pack, int nArgs) {
		MainPack info = (MainPack) pack;
		return info.res.getString(R.string.help_keepn);
	}

	@Override
	public String[] parameters() {
		return null;
	}

	private String addToKeep(String note, MainPack info) {

		//check if keep is installed
		PackageManager pm = info.context.getPackageManager();
		if (isKeepInstalled(pm)) {
			keep(note, info);
			return Tuils.EMPTYSTRING;
		}

		return info.res.getString(R.string.keep_not_installed);
	}

	private boolean isKeepInstalled(PackageManager pm) {
		try {
			pm.getPackageInfo("com.google.android.keep", 0);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void keep(String note, MainPack info) {
		//share intent
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, note);
		sendIntent.setType("text/plain");


		//send to keep only
		/**
		 * since keep doesn't have an api, this is work around found here:
		 * https://stackoverflow.com/a/9755553/3578585
		 */
		PackageManager pm = info.context.getPackageManager();
		List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);

		for (int i = 0; i < resInfo.size(); i++) {
			// Extract the label, append it, and repackage it in a LabeledIntent
			ResolveInfo ri = resInfo.get(i);
			String packageName = ri.activityInfo.packageName;
			if (packageName.contains("keep")) {
				sendIntent.setPackage(packageName);
				info.context.startActivity(sendIntent);
			}
		}

	}
}
