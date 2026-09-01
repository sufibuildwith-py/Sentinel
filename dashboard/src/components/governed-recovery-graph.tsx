"use client";

import { memo, useMemo } from "react";
import {
  Background,
  BackgroundVariant,
  Controls,
  Handle,
  MarkerType,
  Position,
  ReactFlow,
  type Edge,
  type Node,
  type NodeProps,
} from "@xyflow/react";
import { Check, Circle, Pause, ShieldX, TriangleAlert } from "lucide-react";
import type { PipelineStageState, PipelineStageView } from "@/lib/pipeline";
import { cn } from "@/lib/utils";

type RecoveryNodeData = { stage: PipelineStageView };
type RecoveryNode = Node<RecoveryNodeData, "recovery">;

const stateTone: Record<PipelineStageState, string> = {
  COMPLETE: "border-emerald-300 bg-emerald-50 text-emerald-700",
  ACTIVE: "border-blue-400 bg-blue-50 text-blue-700 shadow-[0_0_0_4px_rgba(37,99,235,.08)]",
  QUEUED: "border-slate-200 bg-white text-slate-500",
  HELD: "border-amber-300 bg-amber-50 text-amber-700",
  BLOCKED: "border-red-300 bg-red-50 text-red-700",
  FAILED: "border-red-300 bg-red-50 text-red-700",
  SKIPPED: "border-slate-200 bg-slate-50 text-slate-400",
  NOT_APPLICABLE: "border-slate-200 bg-slate-50 text-slate-400",
};

function StateIcon({ state }: { state: PipelineStageState }) {
  if (state === "COMPLETE") return <Check className="size-3.5" />;
  if (state === "BLOCKED") return <ShieldX className="size-3.5" />;
  if (state === "FAILED") return <TriangleAlert className="size-3.5" />;
  if (state === "HELD") return <Pause className="size-3.5" />;
  if (state === "NOT_APPLICABLE" || state === "SKIPPED") return <span className="font-mono text-[9px]">—</span>;
  return <Circle className="size-3" />;
}

const RecoveryStageNode = memo(function RecoveryStageNode({ data, selected }: NodeProps<RecoveryNode>) {
  const { stage } = data;
  return <div className={cn("w-[126px] rounded-xl border px-3 py-3 shadow-sm transition-[border-color,box-shadow] duration-150", stateTone[stage.state], selected && "ring-2 ring-primary/25") }>
    <Handle type="target" position={Position.Left} className="!size-1.5 !border-0 !bg-slate-300" />
    <Handle type="source" position={Position.Right} className="!size-1.5 !border-0 !bg-slate-300" />
    <div className="flex items-center justify-between gap-2"><span className="font-mono text-[9px] font-semibold tracking-[.12em] uppercase">{stage.label}</span><StateIcon state={stage.state} /></div>
    <p className="mt-2 truncate font-mono text-[8px] uppercase opacity-75">{stage.state.replaceAll("_", " ")}</p>
    {stage.timestamp && <time className="mt-1 block font-mono text-[8px] opacity-55">{new Date(stage.timestamp).toLocaleTimeString()}</time>}
  </div>;
});

const nodeTypes = { recovery: RecoveryStageNode };
const positions: Record<PipelineStageView["label"], { x: number; y: number }> = {
  Detect: { x: 0, y: 52 }, Triage: { x: 160, y: 52 }, Evidence: { x: 320, y: 52 }, Diagnose: { x: 480, y: 52 },
  Counterfactual: { x: 640, y: 52 }, Plan: { x: 800, y: 52 }, Policy: { x: 960, y: 52 }, Human: { x: 1120, y: 140 },
  Governor: { x: 1280, y: 52 }, Execute: { x: 1440, y: 52 }, Accept: { x: 1600, y: 52 }, Reconcile: { x: 1760, y: 52 }, Learn: { x: 1920, y: 52 },
};

export function GovernedRecoveryGraph({ stages, onStageSelect }: { stages: PipelineStageView[]; onStageSelect?: (stage: PipelineStageView) => void }) {
  const nodes = useMemo<RecoveryNode[]>(() => stages.map((stage) => ({ id: stage.label, type: "recovery", position: positions[stage.label], data: { stage }, draggable: false, selectable: true })), [stages]);
  const byLabel = useMemo(() => new Map(stages.map((stage) => [stage.label, stage])), [stages]);
  const edges = useMemo<Edge[]>(() => {
    const humanState = byLabel.get("Human")?.state;
    const authorityPath: Array<[PipelineStageView["label"], PipelineStageView["label"]]> =
      humanState === "NOT_APPLICABLE" || humanState === "SKIPPED"
        ? [["Policy", "Governor"]]
        : [["Policy", "Human"], ["Human", "Governor"]];
    const path: Array<[PipelineStageView["label"], PipelineStageView["label"]]> = [
      ["Detect", "Triage"], ["Triage", "Evidence"], ["Evidence", "Diagnose"], ["Diagnose", "Counterfactual"],
      ["Counterfactual", "Plan"], ["Plan", "Policy"], ...authorityPath,
      ["Governor", "Execute"], ["Execute", "Accept"], ["Accept", "Reconcile"], ["Reconcile", "Learn"],
    ];
    return path.map(([source, target]) => {
      const sourceState = byLabel.get(source)?.state;
      const targetState = byLabel.get(target)?.state;
      const blocked = sourceState === "BLOCKED" || sourceState === "FAILED";
      const traversed = ["COMPLETE", "ACTIVE"].includes(sourceState ?? "") && ["COMPLETE", "ACTIVE", "HELD"].includes(targetState ?? "");
      return { id: `${source}-${target}`, source, target, type: "smoothstep", animated: targetState === "ACTIVE", markerEnd: { type: MarkerType.ArrowClosed, width: 12, height: 12 }, style: { stroke: blocked ? "#ef4444" : traversed ? "#2563eb" : "#cbd5e1", strokeWidth: traversed ? 1.6 : 1 } };
    });
  }, [byLabel]);

  return <div className="h-[330px] w-full overflow-hidden rounded-xl border border-slate-200 bg-white/65" aria-label="Governed recovery graph">
    <ReactFlow nodes={nodes} edges={edges} nodeTypes={nodeTypes} fitView fitViewOptions={{ padding: .12, minZoom: .48, maxZoom: .82 }} minZoom={.4} maxZoom={1.2} nodesDraggable={false} nodesConnectable={false} elementsSelectable panOnScroll zoomOnScroll={false} zoomOnPinch onNodeClick={(_, node) => onStageSelect?.(node.data.stage)} proOptions={{ hideAttribution: false }}>
      <Background variant={BackgroundVariant.Dots} color="#dbe3ef" gap={18} size={1} />
      <Controls showInteractive={false} position="bottom-right" />
    </ReactFlow>
  </div>;
}
