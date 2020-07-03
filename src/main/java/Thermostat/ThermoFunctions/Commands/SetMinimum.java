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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Thermostat.ThermoFunctions.Functions.parseMention;

/**
 * Specific command that sets the minimum slowmode
 * value for channels.
 */
public class SetMinimum extends ListenerAdapter {
    public static EmbedBuilder embed = new EmbedBuilder();

    public void onGuildMessageReceived(GuildMessageReceivedEvent ev) {
// gets given arguments and passes them to a list
        ArrayList<String> args = new ArrayList<>(Arrays.asList(ev.getMessage().getContentRaw().split("\\s+")));

        if (
                args.get(0).equalsIgnoreCase(Thermostat.thermostat.prefix + "setminimum") ||
                        args.get(0).equalsIgnoreCase(Thermostat.thermostat.prefix + "setmin") ||
                        args.get(0).equalsIgnoreCase(Thermostat.thermostat.prefix + "smn")
        ) {
            // checks if member sending request is a bot
            if (ev.getMember().getUser().isBot()) {
                return;
            }

            if (args.size() <= 2) {
                Messages.sendMessage(ev.getChannel(), Embeds.bothChannelAndSlow(ev.getAuthor().getId()));
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
            for (int index = 0; index < args.size() - 1; ++index) {
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
                        args.add(0, it.getId());
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

            for (int index = 0; index < args.size() - 1; ++index) {
                try {
                    // silent guild adder
                    if (!DataSource.checkDatabaseForData("SELECT * FROM GUILDS WHERE GUILD_ID = " + ev.getGuild().getId()))
                        Create.Guild(ev.getGuild().getId());
                    // check db if channel exists and create it if not
                    if (!DataSource.checkDatabaseForData("SELECT * FROM CHANNELS WHERE CHANNEL_ID = " + args.get(index))) {
                        Create.Channel(ev.getGuild().getId(), args.get(index), 1);
                        embed.addField("", "<#" + args.get(index) + "> is now being monitored.\n", false);
                    }

                    int maximumSlow;

                    {
                        // gets minimum value from DB
                        maximumSlow = DataSource.queryInt("SELECT MAX_SLOW FROM CHANNEL_SETTINGS WHERE CHANNEL_ID = " + args.get(index));
                    }

                    if (Integer.parseUnsignedInt(args.get(args.size() - 1)) > maximumSlow)
                    {
                        embed.addField("", "Provided minimum slowmode for <#" + args.get(index) + "> is too high! It should be less than or equal to " + maximumSlow + "!", false);
                        break;
                    } else {
                        DataSource.update("UPDATE CHANNEL_SETTINGS SET MIN_SLOW = " + Integer.parseUnsignedInt(args.get(args.size() - 1)) + " WHERE CHANNEL_ID = " + args.get(index));
                        embed.addField("", "<#" + args.get(index) + "> new minimum slowmode: " + args.get(args.size() - 1) + ".", false);
                    }
                } catch (NumberFormatException ex) {
                    embed.addField("Please insert a valid slowmode value!", "Your inserted value was not appropriate.", false);
                    break;
                }
                catch (Exception ex) {
                    embed.addField("", "Channel " + args.get(index) + " was not found in this guild.\n", false);
                }
            }

            embed.setColor(0xeb9834);
            embed.addField("", "<@" + ev.getAuthor().getId() + ">", false);
            Messages.sendMessage(ev.getChannel(), embed);
            embed.clear();
        }
    }
}