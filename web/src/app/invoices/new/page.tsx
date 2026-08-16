import { FileText } from "lucide-react";
import { ComingSoon } from "@/components/ui/ComingSoon";

export default function NewInvoicePage() {
  return (
    <ComingSoon
      icon={FileText}
      title="New Invoice"
      description="The invoice builder is next up — coming soon."
    />
  );
}
