package contractor.data.scripts.plugins;

import contractor.data.scripts.util.ContractorStaticVars;
import lunalib.lunaSettings.LunaSettingsListener;
import org.jetbrains.annotations.NotNull;

public class ContractorLunaListener implements LunaSettingsListener {
	@Override
	public void settingsChanged(@NotNull String s) {
		ContractorStaticVars.loadValues();
	}
}
