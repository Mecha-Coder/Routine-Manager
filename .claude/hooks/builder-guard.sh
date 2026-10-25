#!/bin/bash
# PreToolUse guard for the milestone-builder subagent (.claude/agents/milestone-builder.md).
# It runs only while a builder runs. It blocks commands and file edits that belong to the
# supervisor or the owner. Exit 2 blocks the tool call; stderr is shown to the builder.

input=$(cat)
tool=$(jq -r '.tool_name // empty' <<<"$input")
root=${CLAUDE_PROJECT_DIR:-$(jq -r '.cwd // empty' <<<"$input")}

block() {
  echo "Blocked for builders: $1. Do not work around this. Stop and report it to the supervisor." >&2
  exit 2
}

has() { grep -qE "$1" <<<"$cmd"; }

case "$tool" in
  Bash)
    cmd=$(jq -r '.tool_input.command // empty' <<<"$input")
    s='(^|[;&|({[:space:]])'   # start of a command word

    has "${s}git([[:space:]]+(-C|-c)[[:space:]]+[^[:space:]]+|[[:space:]]+-[^[:space:]]*)*[[:space:]]+(commit|push|reset|rebase|merge|cherry-pick|revert|filter-branch|filter-repo|clean|stash|tag|remote|config|update-ref|checkout|switch|restore|branch)([[:space:]]|$)" \
      && block "git history, branch, remote and config changes belong to the supervisor"
    has "${s}gh[[:space:]]" && block "GitHub actions belong to the supervisor"

    has "${s}adb([[:space:]].*)?[[:space:]](uninstall|reboot|root|unroot|remount|sideload|disable-verity)([[:space:]]|$)" \
      && block "uninstalling apps and rebooting or rooting the phone need the owner"
    has "pm[[:space:]]+(uninstall|clear|disable|disable-user|hide|suspend)([[:space:]]|$)" && block "removing apps or app data needs the owner"
    has "cmd[[:space:]]+package[[:space:]]+(uninstall|clear)" && block "removing apps or app data needs the owner"
    has "settings[[:space:]]+(put|delete|reset)([[:space:]]|$)" && block "changing phone settings needs the owner"
    has "wm[[:space:]]+(size|density)[[:space:]]+[[:alnum:]]" && block "changing the phone's display size needs the owner"
    has "svc[[:space:]]+(wifi|data|power|usb|bluetooth)" && block "changing phone connectivity or power needs the owner"

    has "${s}rm[[:space:]]+(-[[:alnum:]]*[rR]|--recursive)" && block "recursive deletes need the supervisor (use ./gradlew clean for build output)"
    has "${s}find[[:space:]].*-delete" && block "bulk deletes need the supervisor"
    has "${s}(sudo|keytool)([[:space:]]|$)" && block "sudo and keystore commands belong to the owner"
    has "${s}brew[[:space:]]+(install|uninstall|upgrade|remove|reinstall)" && block "installing Mac software needs the owner"
    has "(curl|wget)[^|]*\|[[:space:]]*(sudo[[:space:]]+)?(ba|z)?sh" && block "piping downloads into a shell is not allowed"

    p='(\.claude/(hooks|agents|skills|settings)|(^|[[:space:]"'\''/])design/|CLAUDE\.md|docs/SPEC\.md|docs/ARCHITECTURE\.md)'
    if has "$p"; then
      has "(sed|perl)[[:space:]]+(-[[:alnum:]]*[[:space:]]+)*-i|${s}(tee|mv|rm|chmod|truncate|ln)[[:space:]]" \
        && block "the guard, the design handoff, CLAUDE.md, SPEC and ARCHITECTURE are changed only by the supervisor"
      has ">[[:space:]]*[\"']?[^[:space:]]*$p" \
        && block "the guard, the design handoff, CLAUDE.md, SPEC and ARCHITECTURE are changed only by the supervisor"
    fi
    ;;
  Edit|Write|NotebookEdit)
    path=$(jq -r '.tool_input.file_path // .tool_input.notebook_path // empty' <<<"$input")
    # A builder's own worktree (.claude/worktrees/<name>/) is checked like the project root.
    case "$path" in
      */.claude/worktrees/*/*) rel=${path#*/.claude/worktrees/*/} ;;
      "$root"/*) rel=${path#"$root"/} ;;
      *) rel=$path ;;
    esac
    case "$rel" in
      .claude/*|*/.claude/*|design/*|CLAUDE.md|docs/SPEC.md|docs/ARCHITECTURE.md)
        block "the guard, the design handoff, CLAUDE.md, SPEC and ARCHITECTURE are changed only by the supervisor; propose the change in your report" ;;
    esac
    ;;
esac
exit 0
