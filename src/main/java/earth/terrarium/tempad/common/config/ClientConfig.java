package earth.terrarium.tempad.common.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;
import earth.terrarium.tempad.client.screen.tempad.Sorting;

@Config("tempad-client")
public class ClientConfig {

    @ConfigEntry(id = "sorting_mode", type = EntryType.STRING)
    @Comment("The sorting mode for the Tempad screen. Options: Dimension, Alphabetical, Type")
    public static String sortingMode = Sorting.Dimension.name();
}
