"use client";

import "@xyflow/react/dist/style.css";

import {
  Background,
  BackgroundVariant,
  Handle,
  Position,
  ReactFlow,
  useEdgesState,
  useNodesState,
  type Edge,
  type Node,
  type NodeProps,
} from "@xyflow/react";
import { useInView, useReducedMotion } from "motion/react";
import { useEffect, useRef } from "react";
import { MonoLabel } from "@/components/pitch/shared/MonoLabel";
import { SceneWrapper } from "@/components/pitch/shared/SceneWrapper";

interface SentinelNodeData extends Record<string, unknown> {
  active: boolean;
  label: string;
  policy?: boolean;
}

type SentinelNode = Node<SentinelNodeData, "sentinel">;

const initialNodes: SentinelNode[] = [
  { id: "failure", position: { x: 300, y: 0 }, data: { label: "PAYMENT FAILED\n₹4,299", active: false }, type: "sentinel" },
  { id: "triage", position: { x: 300, y: 120 }, data: { label: "TRIAGE AGENT", active: false }, type: "sentinel" },
  { id: "evidence", position: { x: 80, y: 260 }, data: { label: "EVIDENCE AGENT", active: false }, type: "sentinel" },
  { id: "risk", position: { x: 300, y: 260 }, data: { label: "RISK AGENT", active: false }, type: "sentinel" },
  { id: "recovery", position: { x: 520, y: 260 }, data: { label: "RECOVERY AGENT", active: false }, type: "sentinel" },
  { id: "policy", position: { x: 300, y: 400 }, data: { label: "POLICY ENGINE", active: false, policy: true }, type: "sentinel" },
];

const initialEdges: Edge[] = [
  { id: "f-t", source: "failure", target: "triage" },
  { id: "t-e", source: "triage", target: "evidence" },
  { id: "t-r", source: "triage", target: "risk" },
  { id: "t-c", source: "triage", target: "recovery" },
  { id: "e-p", source: "evidence", target: "policy" },
  { id: "r-p", source: "risk", target: "policy" },
  { id: "c-p", source: "recovery", target: "policy" },
];

const activationOrder = ["failure", "triage", "evidence", "risk", "recovery", "policy"];

function SentinelFlowNode({ data }: NodeProps<SentinelNode>) {
  const activeClasses = data.policy
    ? "border-blue-600 text-[#f5f5f5] shadow-[0_0_20px_rgba(37,99,235,0.3)]"
    : "border-blue-600/60 text-[#f5f5f5]";

  return (
    <div className={`min-w-40 border bg-[#0f0f0f] px-5 py-3 text-center font-mono text-xs tracking-widest whitespace-pre-line transition-[border-color,color,box-shadow] duration-400 ease-out ${data.active ? activeClasses : "border-white/[0.06] text-[#444444]"}`}>
      <Handle type="target" position={Position.Top} className="!size-1 !border-0 !bg-transparent !opacity-0" />
      {data.label}
      <Handle type="source" position={Position.Bottom} className="!size-1 !border-0 !bg-transparent !opacity-0" />
    </div>
  );
}

const nodeTypes = { sentinel: SentinelFlowNode };

export default function Scene3Pipeline() {
  const shouldReduceMotion = useReducedMotion();
  const sceneRef = useRef<HTMLDivElement>(null);
  const sceneInView = useInView(sceneRef, { once: true, amount: 0.25 });
  const [nodes, setNodes, onNodesChange] = useNodesState<SentinelNode>(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>(initialEdges);

  useEffect(() => {
    if (!sceneInView) return;
    if (shouldReduceMotion) {
      setNodes((current) => current.map((node) => ({ ...node, data: { ...node.data, active: true } })));
      return;
    }

    const timers = activationOrder.map((nodeId, index) => window.setTimeout(() => {
      setNodes((current) => current.map((node) => node.id === nodeId ? { ...node, data: { ...node.data, active: true } } : node));
    }, index * 600));

    return () => timers.forEach((timer) => window.clearTimeout(timer));
  }, [sceneInView, setNodes, shouldReduceMotion]);

  useEffect(() => {
    const activeIds = new Set(nodes.filter((node) => node.data.active).map((node) => node.id));
    setEdges((current) => current.map((edge) => {
      const active = activeIds.has(edge.source) && activeIds.has(edge.target);
      return {
        ...edge,
        animated: active && !shouldReduceMotion,
        className: active
          ? "[&_.react-flow__edge-path]:!stroke-[#2563eb] [&_.react-flow__edge-path]:[stroke-dasharray:6_4]"
          : "[&_.react-flow__edge-path]:!stroke-[#2a2a2a]",
      };
    }));
  }, [nodes, setEdges, shouldReduceMotion]);

  return (
    <SceneWrapper id="scene-3" className="w-full">
      <div ref={sceneRef} className="flex w-full max-w-4xl flex-col items-center">
        <MonoLabel className="text-[#444444]">Intelligence pipeline</MonoLabel>
        <div className="mt-8 w-full max-w-2xl border border-white/[0.06] bg-[#0f0f0f] px-6 py-4 font-mono">
          <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
            <div><p className="text-xs tracking-widest text-[#ef4444]">PAYMENT FAILED</p><p className="mt-2 text-2xl text-[#f5f5f5]">₹4,299</p></div>
            <p className="max-w-md text-xs leading-6 text-[#888888] sm:text-right">Gateway timeout · Customer tenure: 18 months · Previous payments: 13 · Risk: LOW</p>
          </div>
        </div>

        <div className="mt-8 h-[500px] w-full max-w-2xl border border-white/[0.06] bg-[#0a0a0a] [&_.react-flow__attribution]:hidden [&_.react-flow__pane]:cursor-default">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            fitView
            fitViewOptions={{ padding: 0.16 }}
            zoomOnScroll={false}
            zoomOnPinch={false}
            zoomOnDoubleClick={false}
            panOnDrag={false}
            nodesDraggable={false}
            nodesConnectable={false}
            elementsSelectable={false}
            preventScrolling={false}
          >
            <Background variant={BackgroundVariant.Dots} color="#1a1a1a" gap={22} size={1} />
          </ReactFlow>
        </div>
      </div>
    </SceneWrapper>
  );
}
