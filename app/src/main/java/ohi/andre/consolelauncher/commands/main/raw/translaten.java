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

public class translaten implements CommandAbstraction {
	@Override
	public String exec(ExecutePack pack) throws Exception {

		MainPack info = (MainPack) pack;

		String textToTranslate = info.get(String.class, 0);
		return addToTranslate(textToTranslate, info);
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
		return R.string.help_translaten;
	}

	@Override
	public String onArgNotFound(ExecutePack pack) {
		MainPack info = (MainPack) pack;
		return info.res.getString(R.string.help_translaten);
	}

	@Override
	public String onNotArgEnough(ExecutePack pack, int nArgs) {
		MainPack info = (MainPack) pack;
		return info.res.getString(R.string.help_translaten);
	}

	@Override
	public String[] parameters() {
		return null;
	}

	private String addToTranslate(String text, MainPack info) {

		//check if translate is installed
		PackageManager pm = info.context.getPackageManager();
		if (isTranslateInstalled(pm)) {
			translate(text, info);
			return Tuils.EMPTYSTRING;
		}

		return info.res.getString(R.string.translate_not_installed);
	}

	private boolean isTranslateInstalled(PackageManager pm) {
		try {
			pm.getPackageInfo("com.google.android.apps.translate", 0);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void translate(String text, MainPack info) {
		//share intent
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, text);
		sendIntent.setType("text/plain");


		//send to google translate only
		/**
		 * since google translate doesn't have an api, this is work around found here:
		 * https://stackoverflow.com/a/9755553/3578585
		 */
		PackageManager pm = info.context.getPackageManager();
		List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);

		for (int i = 0; i < resInfo.size(); i++) {
			// Extract the label, append it, and repackage it in a LabeledIntent
			ResolveInfo ri = resInfo.get(i);
			String packageName = ri.activityInfo.packageName;
			if (packageName.contains("translate")) {
				sendIntent.setPackage(packageName);
				info.context.startActivity(sendIntent);
			}
		}

	}
}
