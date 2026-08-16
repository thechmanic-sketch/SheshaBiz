import { Receipt } from "lucide-react";
import { ComingSoon } from "@/components/ui/ComingSoon";

export default function NewQuotePage() {
  return (
    <ComingSoon
      icon={Receipt}
      title="New Quote"
      description="The quote builder is next up — coming soon."
    />
  );
}
