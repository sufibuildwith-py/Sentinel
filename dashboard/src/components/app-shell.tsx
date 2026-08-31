"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { AnimatePresence, motion } from "motion/react";
import { Activity, BarChart3, BrainCircuit, ClipboardCheck, FlaskConical, Gauge, ListChecks, Search, ShieldCheck, Sparkles } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { useStatusIsland } from "./providers";
import { Button } from "@/components/ui/button";
import { CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandShortcut } from "@/components/ui/command";
import { Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarHeader, SidebarInset, SidebarMenu, SidebarMenuButton, SidebarMenuItem, SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";

const navigation = [
  { href: "/console", label: "Overview", icon: Gauge },
  { href: "/recovery", label: "Recovery", icon: Sparkles },
  { href: "/incidents", label: "Incidents", icon: Activity },
  { href: "/approvals", label: "Review queue", icon: ListChecks },
  { href: "/intelligence", label: "Intelligence", icon: BrainCircuit },
  { href: "/governance", label: "Governance", icon: ShieldCheck },
  { href: "/evaluation", label: "Evaluation", icon: BarChart3 },
  { href: "/audit", label: "Audit", icon: ClipboardCheck },
  { href: "/demo", label: "Failure Lab", icon: FlaskConical },
];

function StatusIsland() {
  const { event } = useStatusIsland();
  return <motion.div layout className="pointer-events-none absolute left-1/2 top-2 z-20 -translate-x-1/2" aria-live="polite">
    <motion.div layout className="flex min-h-9 items-center gap-2 rounded-full border border-slate-200/80 bg-white/85 px-3 shadow-lg shadow-slate-900/5 backdrop-blur-xl">
      <span className="status-dot size-1.5 shrink-0 rounded-full bg-primary" />
      <AnimatePresence mode="popLayout" initial={false}>
        {event ? <motion.div key={event.title} initial={{ opacity: 0, width: 0 }} animate={{ opacity: 1, width: "auto" }} exit={{ opacity: 0, width: 0 }} className="flex max-w-[62vw] items-center gap-2 overflow-hidden whitespace-nowrap">
          <span className="text-xs font-semibold">{event.title}</span><span className="hidden text-xs text-muted-foreground sm:inline">{event.detail}</span>
        </motion.div> : <motion.span key="ready" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="text-[11px] font-medium text-muted-foreground">Sentinel ready</motion.span>}
      </AnimatePresence>
    </motion.div>
  </motion.div>;
}

function CommandPalette() {
  const [open, setOpen] = useState(false);
  const router = useRouter();
  useEffect(() => { const onKey = (event: KeyboardEvent) => { if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") { event.preventDefault(); setOpen((value) => !value); } }; document.addEventListener("keydown", onKey); return () => document.removeEventListener("keydown", onKey); }, []);
  const go = (href: string) => { setOpen(false); router.push(href); };
  return <><Button variant="ghost" size="sm" onClick={() => setOpen(true)} aria-label="Open command palette"><Search /> <span className="hidden md:inline">Search</span><kbd className="ml-2 hidden rounded border border-slate-200 px-1.5 py-0.5 text-[10px] text-muted-foreground lg:inline">⌘K</kbd></Button>
    <CommandDialog open={open} onOpenChange={setOpen} title="Sentinel command palette"><CommandInput placeholder="Go to a workspace…" /><CommandList><CommandEmpty>No command found.</CommandEmpty><CommandGroup heading="Navigate">{navigation.map((item) => <CommandItem key={item.href} value={item.label} onSelect={() => go(item.href)}><item.icon />{item.label}{item.href === "/incidents" && <CommandShortcut>I</CommandShortcut>}</CommandItem>)}</CommandGroup></CommandList></CommandDialog></>;
}

export function AppShell({ children }: { children: ReactNode }) {
  const path = usePathname();
  return <SidebarProvider><Sidebar collapsible="icon" variant="inset" className="border-r border-slate-200/80">
    <SidebarHeader className="p-3"><div className="flex h-11 items-center gap-3 rounded-xl px-2"><div className="grid size-8 shrink-0 place-items-center rounded-[10px] border border-primary/30 bg-primary/10 text-primary"><ShieldCheck className="size-4" /></div><div className="overflow-hidden"><p className="truncate text-sm font-semibold tracking-tight">Sentinel</p><p className="truncate text-[10px] text-muted-foreground">Revenue intelligence</p></div></div></SidebarHeader>
    <SidebarContent><SidebarGroup><SidebarGroupLabel>Command center</SidebarGroupLabel><SidebarGroupContent><SidebarMenu>{navigation.map((item) => { const active = item.href === "/console" ? path === "/console" : path.startsWith(item.href); return <SidebarMenuItem key={item.href}><SidebarMenuButton render={<Link href={item.href} />} isActive={active} tooltip={item.label}><item.icon /><span>{item.label}</span></SidebarMenuButton></SidebarMenuItem>; })}</SidebarMenu></SidebarGroupContent></SidebarGroup></SidebarContent>
    <SidebarFooter className="p-3"><div className="rounded-xl border border-primary/15 bg-primary/5 p-3 group-data-[collapsible=icon]:hidden"><p className="text-[10px] font-bold tracking-[.14em] text-primary">GOVERNED OPERATIONS</p><p className="mt-1 text-[11px] leading-4 text-muted-foreground">Provider truth is always labelled. Models never grant authority.</p></div></SidebarFooter>
  </Sidebar><SidebarInset className="console-grid min-w-0 overflow-hidden">
    <header className="sticky top-0 z-40 relative flex h-14 shrink-0 items-center justify-between border-b border-slate-200/80 bg-white/75 px-3 backdrop-blur-xl sm:px-5"><div className="flex items-center gap-2"><SidebarTrigger /><div className="hidden h-4 w-px bg-border sm:block" /><span className="hidden text-xs text-muted-foreground sm:block">Operations command center</span></div><StatusIsland /><div className="flex items-center gap-2"><CommandPalette /><div className="hidden xl:block"><span className="test-label">Truth-labelled console</span></div></div></header>
    <main className="mx-auto w-full max-w-[1540px] flex-1 p-4 sm:p-6 lg:p-8">{children}</main>
  </SidebarInset></SidebarProvider>;
}
