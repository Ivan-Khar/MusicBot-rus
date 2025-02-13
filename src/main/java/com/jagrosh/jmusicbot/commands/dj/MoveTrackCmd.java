package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.DJCommand;
import com.jagrosh.jmusicbot.queue.FairQueue;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.LinkedList;
import java.util.List;

/**
 * Command that provides users the ability to move a track in the playlist.
 */
public class MoveTrackCmd extends DJCommand {

  public MoveTrackCmd(Bot bot) {
    super(bot);
    this.name = "movetrack";
    this.help = "двигает пластинку в другое место в очереди";
    this.arguments = "<from> <to>";
    List<OptionData> options = new LinkedList<>();
    options.add(new OptionData(OptionType.INTEGER, "from", "Позиция пластинки в очереди").setRequired(true));
    options.add(new OptionData(OptionType.INTEGER, "to", "Позиция куда поместить пластинку").setRequired(true));
    this.options = options;
    this.aliases = bot.getConfig().getAliases(this.name);
    this.bePlaying = true;
  }

  @Override
  public void doCommand(CommandEvent event) {
    int from;
    int to;

    String[] parts = event.getArgs().split("\\s+", 2);
    if (parts.length < 2) {
      event.replyError("напишите два положения в очереди.");
      return;
    }

    try {
      // Validate the args
      from = Integer.parseInt(parts[0]);
      to = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      event.replyError("Напишите два положения в очереди.");
      return;
    }

    if (from == to) {
      event.replyError("Не могу передвинуть пластинку на свое же место.");
      return;
    }

    // Validate that from and to are available
    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
    FairQueue<QueuedTrack> queue = handler.getQueue();
    if (isUnavailablePosition(queue, from)) {
      String reply = String.format("`%d` положение в очереди не правильное!", from);
      event.replyError(reply);
      return;
    }
    if (isUnavailablePosition(queue, to)) {
      String reply = String.format("`%d` положение в очереди не правильное!", to);
      event.replyError(reply);
      return;
    }

    // Move the track
    QueuedTrack track = queue.moveItem(from - 1, to - 1);
    String trackTitle = track.getTrack().getInfo().title;
    String reply = String.format("Передвинули **%s** с позиции `%d` на позицию `%d`.", trackTitle, from, to);
    event.replySuccess(reply);
  }

  @Override
  public void doSlashCommand(SlashCommandEvent event) {
    int from = event.getOption("from").getAsInt();
    int to = event.getOption("to").getAsInt();

    if (from == to) {
      event.getHook().editOriginal("Не могу передвинуть пластинку на свое же место.")
        .queue();
      return;
    }

    // Validate that from and to are available
    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
    FairQueue<QueuedTrack> queue = handler.getQueue();
    if (isUnavailablePosition(queue, from)) {
      String reply = String.format("`%d` положение в очереди не правильное!", from);
      event.getHook().editOriginal(reply)
        .queue();
      return;
    }
    if (isUnavailablePosition(queue, to)) {
      String reply = String.format("`%d` положение в очереди не правильное!", to);
      event.getHook().editOriginal(reply)
        .queue();
      return;
    }

    // Move the track
    QueuedTrack track = queue.moveItem(from - 1, to - 1);
    String trackTitle = track.getTrack().getInfo().title;
    String reply = String.format("Передвинули **%s** с позиции `%d` на позицию `%d`.", trackTitle, from, to);
    event.getHook().editOriginal(reply)
      .queue();

  }

  private static boolean isUnavailablePosition(FairQueue<QueuedTrack> queue, int position) {
    return (position < 1 || position > queue.size());
  }
}
