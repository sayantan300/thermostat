package Thermostat.ThermoFunctions.Commands;

import Thermostat.Embeds;
import Thermostat.MySQL.Create;
import Thermostat.MySQL.DataSource;
import Thermostat.ThermoFunctions.Messages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Thermostat.ThermoFunctions.Functions.parseMention;

/**
 * <h1>Monitor Command</h1>
 * <p>
 * Adds channels to the database provided in
 * db.properties, upon user running the
 * command. Extends ListenerAdapter thus must
 * be added as a listener in {@link Thermostat.thermostat}.
 */
public class Monitor extends ListenerAdapter {
    private static EmbedBuilder embed = new EmbedBuilder();

    public void onGuildMessageReceived(GuildMessageReceivedEvent ev) {
        // gets given arguments and passes them to a list
        ArrayList<String> args = new ArrayList<>(Arrays.asList(ev.getMessage().getContentRaw().split("\\s+")));

        if (
                args.get(0).equalsIgnoreCase(Thermostat.thermostat.prefix + "monitor") ||
                        args.get(0).equalsIgnoreCase(Thermostat.thermostat.prefix + "mon") ||
                        args.get(0).equalsIgnoreCase(Thermostat.thermostat.prefix + "m")
        ) {
            // checks if member sending request is a bot
            if (ev.getMember().getUser().isBot()) {
                return;
            }

            if (args.size() == 1) {
                Messages.sendMessage(ev.getChannel(), Embeds.specifyChannels(ev.getAuthor().getId()));
                return;
            }

            // catch to remove command initiation with prefix
            args.remove(0);

            // checks if event member has permission
            if (!ev.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                Messages.sendMessage(ev.getChannel(), Embeds.userNoPermission(ev.getAuthor().getId()));
                return;
            }

            embed.setTitle("ℹ Command Results:");

            // parses arguments into usable IDs, checks if channels exist
            // realIndex is just for the message.
            int realIndex = 1;
            for (int index = 0; index < args.size(); ++index) {
                // first check, if it's a channel mention then passes id instead
                args.set(index, parseMention(args.get(index), "#"));

                // if string is empty add a 0 to it in order to represent
                // empty channel
                if (args.get(index).isBlank()) {
                    embed.addField("", "Channel #" + realIndex + " is not a valid channel.", false);
                    args.remove(index);
                    --index;
                }

                // if given argument is a category get channels from it
                // and pass them to the arguments ArrayList
                else if (ev.getGuild().getCategoryById(args.get(index)) != null) {
                    // firstly creates an immutable list of the channels in the category
                    List<TextChannel> TextChannels = ev.getGuild().getCategoryById(args.get(index)).getTextChannels();
                    // if list is empty add that it is in msg
                    if (TextChannels.isEmpty()) {
                        embed.addField("", "Category <#" + args.get(index) + "> does not contain any text channels.", false);
                    }
                    // removes category ID from argument ArrayList
                    args.remove(index);
                    // iterates through every channel and adds its' id to the arg list
                    for (TextChannel it : TextChannels) {
                        args.add(it.getId());
                    }
                    --index;
                }

                // removes element from arguments if it's not a valid channel ID
                else if (ev.getGuild().getTextChannelById(args.get(index)) == null) {
                    embed.addField("", "Text Channel " + args.get(index) + " was not found in this guild.", false);
                    args.remove(index);
                    --index;
                }

                ++realIndex;
            }

            // connects to database and creates channel

            for (String it : args) {
                try {
                    // silent guild adder
                    if (!DataSource.checkDatabaseForData("SELECT * FROM GUILDS WHERE GUILD_ID = " + ev.getGuild().getId()))
                        Create.Guild(ev.getGuild().getId());
                    // check db if channel exists
                    if (!DataSource.checkDatabaseForData("SELECT * FROM CHANNELS WHERE CHANNEL_ID = " + it)) {
                        Create.Channel(ev.getGuild().getId(), it, 1);
                        embed.addField("", "<#" + it + "> is now being monitored.\n", false);
                    } else {
                        // checks if the channel is actively being
                        // monitored
                        boolean isMonitor = DataSource.queryBool("SELECT MONITORED FROM CHANNEL_SETTINGS WHERE CHANNEL_ID = " + it);

                        // checks whether the channel has the monitor
                        // value on the database set to 1
                        // table CHANNEL_SETTINGS
                        if (isMonitor)
                        {
                            embed.addField("", "Channel <#" + it + "> is already being monitored.", false);
                        } else {
                            Create.ChannelMonitor(ev.getGuild().getId(), it, 1);
                            embed.addField("", "<#" + it + "> is now being monitored.\n", false);
                        }
                    }
                } catch (Exception ex) {
                    embed.addField("", "Channel " + it + " was not found in this guild.\n", false);
                }
            }

            embed.setColor(0xeb9834);
            embed.addField("", "<@" + ev.getAuthor().getId() + ">", false);
            Messages.sendMessage(ev.getChannel(), embed);
            embed.clear();
        }
    }
}