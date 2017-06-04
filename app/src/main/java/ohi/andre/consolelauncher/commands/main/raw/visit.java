package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by mhashim6 on 04/06/2017.
 */

public class visit implements CommandAbstraction {
	@Override
	public String exec(ExecutePack pack) throws Exception {
		MainPack info = (MainPack) pack;

		String address = info.get(String.class, 0);

		return visitAddress(address, info.context);

	}

	@Override
	public int minArgs() {
		return 1;
	}

	@Override
	public int maxArgs() {
		return 1;
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
		return R.string.help_visit;
	}

	@Override
	public String onArgNotFound(ExecutePack pack) {
		MainPack info = (MainPack) pack;
		return info.res.getString(helpRes());
	}

	@Override
	public String onNotArgEnough(ExecutePack pack, int nArgs) {
		MainPack info = (MainPack) pack;
		return info.res.getString(helpRes());
	}

	@Override
	public String[] parameters() {
		return null;
	}

	private String visitAddress(String url, Context c) {
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "http://" + url;
		}

		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		c.startActivity(intent);

		return Tuils.EMPTYSTRING;
	}
}
