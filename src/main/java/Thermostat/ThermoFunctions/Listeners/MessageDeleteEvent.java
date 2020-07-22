package Thermostat.ThermoFunctions.Listeners;

import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import static Thermostat.ThermoFunctions.Commands.Objects.MonitoredMessage.monitoredMessages;

public class MessageDeleteEvent extends ListenerAdapter
{
    public void onGuildMessageDelete (GuildMessageDeleteEvent event) {
        try {
            monitoredMessages.removeIf(it -> event.getMessageId().equals(it.getMessageId()));
        } catch (Exception ignored) {}
    }
}
