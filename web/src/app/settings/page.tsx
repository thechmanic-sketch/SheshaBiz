import { Settings } from "lucide-react";
import { ComingSoon } from "@/components/ui/ComingSoon";

export default function SettingsPage() {
  return (
    <ComingSoon
      icon={Settings}
      title="Settings"
      description="Business profile, VAT, and account settings — coming soon."
    />
  );
}
